/*
        Licensed to the Apache Software Foundation (ASF) under one
        or more contributor license agreements.  See the NOTICE file
        distributed with this work for additional information
        regarding copyright ownership.  The ASF licenses this file
        to you under the Apache License, Version 2.0 (the
        "License"); you may not use this file except in compliance
        with the License.  You may obtain a copy of the License at
          http://www.apache.org/licenses/LICENSE-2.0
        Unless required by applicable law or agreed to in writing,
        software distributed under the License is distributed on an
        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
        KIND, either express or implied.  See the License for the
        specific language governing permissions and limitations
        under the License.
 */

package org.crosswalk.engine;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.webkit.ValueCallback;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * Created by lundfall on 26/07/2017.
 */

public class XWalkBridgeEngine implements CordovaWebViewEngine {

    CordovaWebViewEngine underlyingEngine;

    public XWalkBridgeEngine(Context context, CordovaPreferences preferences) {
        if (XWalkBridgeEngine.shouldMakeXwalkWebView(context)) {
            underlyingEngine = new XWalkWebViewEngine(context, preferences);
        } else {
            underlyingEngine = new SystemWebViewEngine(context, preferences);
        }
    }

    @Override
    public void init(CordovaWebView parentWebView, CordovaInterface cordova, Client client, CordovaResourceApi resourceApi, PluginManager pluginManager, NativeToJsMessageQueue nativeToJsMessageQueue) {
        underlyingEngine.init(parentWebView, cordova, client, resourceApi, pluginManager, nativeToJsMessageQueue);
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return underlyingEngine.getCordovaWebView();
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        return underlyingEngine.getCookieManager();
    }

    @Override
    public View getView() {
        return underlyingEngine.getView();
    }

    @Override
    public void loadUrl(String url, boolean clearNavigationStack) {
        underlyingEngine.loadUrl(url, clearNavigationStack);
    }

    @Override
    public void stopLoading() {
        underlyingEngine.stopLoading();
    }

    @Override
    public String getUrl() {
        return underlyingEngine.getUrl();
    }

    @Override
    public void clearCache() {
        underlyingEngine.clearCache();
    }

    @Override
    public void clearHistory() {
        underlyingEngine.clearHistory();
    }

    @Override
    public boolean canGoBack() {
        return underlyingEngine.canGoBack();
    }

    @Override
    public boolean goBack() {
        return underlyingEngine.goBack();
    }

    @Override
    public void setPaused(boolean value) {
        underlyingEngine.setPaused(value);
    }

    @Override
    public void destroy() {
        underlyingEngine.destroy();
    }

    @Override
    public void evaluateJavascript(String js, ValueCallback<String> callback) {
        underlyingEngine.evaluateJavascript(js, callback);
    }

    static private boolean checkedShouldMakeXwalkWebView = false;
    static private boolean cachedShouldMakeXwalkWebView = false;

    static public boolean shouldMakeXwalkWebView(Context context) {

        if (XWalkBridgeEngine.checkedShouldMakeXwalkWebView) {
            return XWalkBridgeEngine.cachedShouldMakeXwalkWebView;
        }

        XWalkBridgeEngine.checkedShouldMakeXwalkWebView = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { // only SDK < Lollipop
            XWalkBridgeEngine.cachedShouldMakeXwalkWebView = true;
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // only for Nougat and newer versions
            XWalkBridgeEngine.cachedShouldMakeXwalkWebView = false;
            return false;
        }

        XWalkBridgeEngine.cachedShouldMakeXwalkWebView = true;
        PackageInfo packageInfo = getSystemWebViewPackageInfo(context);
        if (null != packageInfo) {
            String versionName = packageInfo.versionName;
            String majorVersionNumber = versionName.split("\\.")[0];
            try {
                int systemWebViewVersion = Integer.parseInt(majorVersionNumber);
                /* Xwalk will make a web view with chrome version 55, so if the systemWebView has a higher version, it should be used */
                XWalkBridgeEngine.cachedShouldMakeXwalkWebView = systemWebViewVersion < 55;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return XWalkBridgeEngine.cachedShouldMakeXwalkWebView;
    }

    static private PackageInfo getSystemWebViewPackageInfo(Context context) {
        PackageInfo packageInfo = getWebViewPackageInfo(context, "com.android.webview");
        if (null == packageInfo) {
            packageInfo = getWebViewPackageInfo(context, "com.google.android.webview");
        }
        return packageInfo;
    }

    static private PackageInfo getWebViewPackageInfo(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        int enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        try {
            enabledSetting = packageManager.getApplicationEnabledSetting(packageName);
        } catch (IllegalArgumentException e) {
            // if the named package does not exist.
            e.printStackTrace();
        }
        if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            try {
                return context.getPackageManager().getPackageInfo(packageName, 0);
                /* Parse exceptions generate true */
            } catch (RuntimeException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
