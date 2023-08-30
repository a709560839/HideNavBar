package com.jj.hidenavbar.xposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage {

    public static int DISABLE_EXPAND = -1;
    private boolean mHideGestureLine;
    private boolean sIsNeedInjectMotionEvent;
    private MotionEvent motionEvent;
    private int mTopMargin;
    private int mCurrentTopMargin;
    private boolean isLandScapeActually;
    private Context context;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            if ("com.miui.home".equals(loadPackageParam.packageName)) {
                //点击事件分发
                if (DISABLE_EXPAND == -1) {
                    Class<?> clazz = XposedHelpers.findClass("com.miui.launcher.utils.StatusBarController", loadPackageParam.classLoader);
                    DISABLE_EXPAND = XposedHelpers.getStaticIntField(clazz, "DISABLE_EXPAND");
                }
                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "injectMotionEvent", int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                sIsNeedInjectMotionEvent = XposedHelpers.getBooleanField(param.thisObject, "sIsNeedInjectMotionEvent");
                                motionEvent = (MotionEvent) XposedHelpers.getObjectField(param.thisObject, "mDownEvent");
                                if ((motionEvent.getFlags() & DISABLE_EXPAND) == 0) {
                                    XposedHelpers.setBooleanField(param.thisObject, "sIsNeedInjectMotionEvent", true);
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                if ((motionEvent.getFlags() & DISABLE_EXPAND) == 0) {
                                    XposedHelpers.setBooleanField(param.thisObject, "sIsNeedInjectMotionEvent", sIsNeedInjectMotionEvent);
                                }
                            }
                        });

                //获取初始横竖屏状态
                findAndHookConstructor("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, Context.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                context = (Context) param.args[0];
                                isLandScapeActually = (boolean) XposedHelpers.callMethod(param.thisObject, "isLandScapeActually");
                            }
                        });

                //旋转屏幕时更新横竖屏状态
                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "onConfigurationChanged", Configuration.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                isLandScapeActually = (boolean) XposedHelpers.callMethod(param.thisObject, "isLandScapeActually");
                            }
                        });

                //二次滑动确认时上滑显示的view,显示时再hook
                findAndHookConstructor("com.miui.home.recents.AntiMistakeTouchView", loadPackageParam.classLoader, Context.class, AttributeSet.class, int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                mTopMargin = XposedHelpers.getIntField(param.thisObject, "mTopMargin");
                                mCurrentTopMargin = mTopMargin;
                            }
                        });
                findAndHookMethod("com.miui.home.recents.AntiMistakeTouchView", loadPackageParam.classLoader, "slideUp",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                mCurrentTopMargin = 0;
                            }
                        });
                findAndHookMethod("com.miui.home.recents.AntiMistakeTouchView", loadPackageParam.classLoader, "slideDown",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                mCurrentTopMargin = mTopMargin;
                            }
                        });

                //滑动事件
                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "onTouchEvent", MotionEvent.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                mHideGestureLine = XposedHelpers.getBooleanField(param.thisObject, "mHideGestureLine");
                                //横屏
                                if (isLandScapeActually) {
                                    //需要二次滑动确认
                                    if (Settings.Global.getInt(context.getContentResolver(), "show_mistake_touch_toast", 1) == 1) {
                                        //二次滑动确认时上滑的view已经显示出来
                                        if (mCurrentTopMargin == 0) {
                                            XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", false);
                                        }
                                    } else {
                                        XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", false);
                                    }
                                }
                                //竖屏
                                else {
                                    //二次滑动确认打开了，但是不像横屏一样显示一个view以及再划一次，是我机子的问题还是本身逻辑如此？
                                    XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", false);
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", mHideGestureLine);
                            }
                        });


                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "getWindowParam", int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                mHideGestureLine = XposedHelpers.getBooleanField(param.thisObject, "mHideGestureLine");
                                XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", false);
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine", mHideGestureLine);
                            }

                        });
            }
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }
}
