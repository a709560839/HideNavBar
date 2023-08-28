package com.jj.hidenavbar.xposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage {

    private boolean mHideGestureLine;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            if ("com.miui.home".equals(loadPackageParam.packageName)) {
                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "getHotSpaceHeight",
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                return 20;
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

                findAndHookMethod("com.miui.home.recents.NavStubView", loadPackageParam.classLoader, "onTouchEvent", android.view.MotionEvent.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                mHideGestureLine = XposedHelpers.getBooleanField(param.thisObject, "mHideGestureLine");
                                XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine",false);
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                XposedHelpers.setBooleanField(param.thisObject, "mHideGestureLine",mHideGestureLine);
                            }
                        });
            }
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }
}
