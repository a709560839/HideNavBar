package com.jj.hidenavbar.xposed;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class HookInit extends XposedModule {

    private static final String TAG = "MIUI HideNavBar";

    public static int DISABLE_EXPAND = -1;
    private boolean mHideGestureLine;
    private boolean sIsNeedInjectMotionEvent;
    private boolean hasSIsNeedInjectMotionEvent = true;
    private MotionEvent motionEvent;
    private int mTopMargin;
    private int mCurrentTopMargin;
    private boolean isLandScapeActually;
    private Context context;


    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!"com.miui.home".equals(param.getPackageName())) {
            return;
        }

        try {
            ClassLoader classLoader = param.getClassLoader();

            // 点击事件分发
            if (DISABLE_EXPAND == -1) {
                Class<?> clazz = Class.forName("com.miui.launcher.utils.StatusBarController", true, classLoader);
                DISABLE_EXPAND = ReflectUtils.getStaticIntField(clazz, "DISABLE_EXPAND");
            }

            Class<?> navStubViewClass = Class.forName("com.miui.home.recents.NavStubView", true, classLoader);
            Method injectMotionEventMethod = navStubViewClass.getDeclaredMethod("injectMotionEvent", int.class);
            hook(injectMotionEventMethod).intercept(chain -> {
                Object thisObject = chain.getThisObject();
                if (hasSIsNeedInjectMotionEvent) {
                    try {
                        //旧版系统桌面用sIsNeedInjectMotionEvent判断是否点击
                        sIsNeedInjectMotionEvent = ReflectUtils.getBooleanField(thisObject, "sIsNeedInjectMotionEvent");
                    } catch (Throwable throwable) {
                        hasSIsNeedInjectMotionEvent = false;
                        if (context != null) {
                            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                            log(Log.INFO, TAG, "MIUI隐藏小白条/MIUI HideNavBar:没有sIsNeedInjectMotionEvent，当前系统桌面版本——" + packageInfo.versionName);
                        }
                    }
                }

                try {
                    //新版系统桌面用mHideGestureLine判断是否点击
                    mHideGestureLine = ReflectUtils.getBooleanField(thisObject, "mHideGestureLine");
                    motionEvent = (MotionEvent) ReflectUtils.getObjectField(thisObject, "mDownEvent");

                    if (motionEvent != null && (motionEvent.getFlags() & DISABLE_EXPAND) == 0) {
                        ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", false);
                        if (hasSIsNeedInjectMotionEvent) {
                            ReflectUtils.setBooleanField(thisObject, "sIsNeedInjectMotionEvent", true);
                        }
                    }
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in before injectMotionEvent hook", e);
                }

                Object result = chain.proceed();

                try {
                    if (motionEvent != null && (motionEvent.getFlags() & DISABLE_EXPAND) == 0) {
                        ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", mHideGestureLine);
                        if (hasSIsNeedInjectMotionEvent) {
                            ReflectUtils.setBooleanField(thisObject, "sIsNeedInjectMotionEvent", sIsNeedInjectMotionEvent);
                        }
                    }
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after injectMotionEvent hook", e);
                }

                return result;
            });

            // 获取初始横竖屏状态
            Constructor<?> navStubViewConstructor = navStubViewClass.getDeclaredConstructor(Context.class);
            hook(navStubViewConstructor).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    context = (Context) chain.getArg(0);
                    isLandScapeActually = (boolean) ReflectUtils.callMethodNoArgs(chain.getThisObject(), "isLandScapeActually");
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after navStubViewConstructor hook", e);
                }
                return result;
            });

            // 旋转屏幕时更新横竖屏状态
            Method onConfigurationChangedMethod = navStubViewClass.getDeclaredMethod("onConfigurationChanged", Configuration.class);
            hook(onConfigurationChangedMethod).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    isLandScapeActually = (boolean) ReflectUtils.callMethodNoArgs(chain.getThisObject(), "isLandScapeActually");
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after onConfigurationChanged hook", e);
                }
                return result;
            });

            // 二次滑动确认时上滑显示的view,显示时再hook
            Class<?> antiMistakeTouchViewClass = Class.forName("com.miui.home.recents.AntiMistakeTouchView", true, classLoader);
            Constructor<?> antiMistakeTouchViewConstructor = antiMistakeTouchViewClass.getDeclaredConstructor(Context.class, AttributeSet.class, int.class);
            hook(antiMistakeTouchViewConstructor).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    mTopMargin = ReflectUtils.getIntField(chain.getThisObject(), "mTopMargin");
                    mCurrentTopMargin = mTopMargin;
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after antiMistakeTouchViewConstructor hook", e);
                }
                return result;
            });

            Method slideUpMethod = antiMistakeTouchViewClass.getDeclaredMethod("slideUp");
            hook(slideUpMethod).intercept(chain -> {
                Object result = chain.proceed();
                mCurrentTopMargin = 0;
                return result;
            });

            Method slideDownMethod = antiMistakeTouchViewClass.getDeclaredMethod("slideDown");
            hook(slideDownMethod).intercept(chain -> {
                Object result = chain.proceed();
                mCurrentTopMargin = mTopMargin;
                return result;
            });

            // 滑动事件
            Method onTouchEventMethod = navStubViewClass.getDeclaredMethod("onTouchEvent", MotionEvent.class);
            hook(onTouchEventMethod).intercept(chain -> {
                Object thisObject = chain.getThisObject();
                try {
                    mHideGestureLine = ReflectUtils.getBooleanField(thisObject, "mHideGestureLine");

                    if (isLandScapeActually) {
                        //需要二次滑动确认
                        if (Settings.Global.getInt(context.getContentResolver(), "show_mistake_touch_toast", 1) == 1) {
                            //二次滑动确认时上滑的view已经显示出来
                            if (mCurrentTopMargin == 0) {
                                ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", false);
                            }
                        } else {
                            ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", false);
                        }
                    } else {
                        // 竖屏
                        //二次滑动确认打开了，但是不像横屏一样显示一个view以及再划一次，是我机子的问题还是本身逻辑如此？
                        ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", false);
                    }
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in before onTouchEvent hook", e);
                }

                Object result = chain.proceed();

                try {
                    ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", mHideGestureLine);
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after onTouchEvent hook", e);
                }

                return result;
            });

            // getWindowParam
            Method getWindowParamMethod = navStubViewClass.getDeclaredMethod("getWindowParam", int.class);
            hook(getWindowParamMethod).intercept(chain -> {
                Object thisObject = chain.getThisObject();
                try {
                    mHideGestureLine = ReflectUtils.getBooleanField(thisObject, "mHideGestureLine");
                    ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", false);
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in before getWindowParam hook", e);
                }

                Object result = chain.proceed();

                try {
                    ReflectUtils.setBooleanField(thisObject, "mHideGestureLine", mHideGestureLine);
                } catch (Exception e) {
                    log(Log.ERROR, TAG, "Error in after getWindowParam hook", e);
                }

                return result;
            });

        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Error during hook initialization", throwable);
        }
    }
}
