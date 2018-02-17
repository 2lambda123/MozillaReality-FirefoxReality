/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include <jni.h>
#include <string>
#include <GLES2/gl2.h>

#include "BrowserWorld.h"
#include "vrb/Logger.h"
#include "vrb/GLError.h"
#include "BrowserEGLContext.h"
#include <android_native_app_glue.h>
#include <cstdlib>
#include <vrb/RunnableQueue.h>
#include "DeviceDelegateOculusVR.h"
#include <android/looper.h>
#include <unistd.h>


#define JNI_METHOD(return_type, method_name) \
  JNIEXPORT return_type JNICALL              \
    Java_org_mozilla_vrbrowser_VRBrowserActivity_##method_name

using namespace crow;

static
jobject GetAssetManager(JNIEnv * aEnv, jobject aActivity) {
  jclass clazz = aEnv->GetObjectClass(aActivity);
  jmethodID method = aEnv->GetMethodID(clazz, "getAssets", "()Landroid/content/res/AssetManager;");
  jobject result =  aEnv->CallObjectMethod(aActivity, method);
  if (!result) {
    VRB_LOG("Failed to get AssetManager instance!");
  }
  return result;
}


struct AppContext {
  vrb::RunnableQueuePtr mQueue;
  BrowserWorldPtr mWorld;
  BrowserEGLContextPtr mEgl;
  DeviceDelegateOculusVRPtr mDevice;
};

void
CommandCallback(android_app *aApp, int32_t aCmd) {
  AppContext *ctx = (AppContext *) aApp->userData;

  switch (aCmd) {
    // A new ANativeWindow is ready for use. Upon receiving this command,
    // android_app->window will contain the new window surface.
    case APP_CMD_INIT_WINDOW:
      VRB_LOG("APP_CMD_INIT_WINDOW %p", aApp->window);
      if (!ctx->mEgl) {
        ctx->mEgl = BrowserEGLContext::Create();
        ctx->mEgl->Initialize(aApp->window);
        ctx->mEgl->MakeCurrent();
        VRB_CHECK(glClearColor(0.0, 0.0, 0.0, 1.0));
        VRB_CHECK(glEnable(GL_DEPTH_TEST));
        VRB_CHECK(glEnable(GL_CULL_FACE));
        ctx->mWorld->InitializeGL();
      } else {
        ctx->mEgl->SurfaceChanged(aApp->window);
        ctx->mEgl->MakeCurrent();
      }

      if (!ctx->mWorld->IsPaused() && !ctx->mDevice->IsInVRMode()) {
        ctx->mDevice->EnterVR(*ctx->mEgl);
      }

      break;

    // The existing ANativeWindow needs to be terminated.  Upon receiving this command,
    // android_app->window still contains the existing window;
    // after calling android_app_exec_cmd it will be set to NULL.
    case APP_CMD_TERM_WINDOW:
      VRB_LOG("APP_CMD_TERM_WINDOW");
      if (ctx->mDevice->IsInVRMode()) {
         ctx->mDevice->LeaveVR();
      }
      if (ctx->mEgl) {
        ctx->mEgl->SurfaceDestroyed();
      }
      break;
    // The app's activity has been paused.
    case APP_CMD_PAUSE:
      VRB_LOG("APP_CMD_PAUSE");
      ctx->mWorld->Pause();
      if (ctx->mDevice->IsInVRMode()) {
        ctx->mDevice->LeaveVR();
      }
      break;

    // The app's activity has been resumed.
    case APP_CMD_RESUME:
      VRB_LOG("APP_CMD_RESUME");
      ctx->mWorld->Resume();
      if (!ctx->mDevice->IsInVRMode() && ctx->mEgl && ctx->mEgl->IsSurfaceReady() ) {
         ctx->mDevice->EnterVR(*ctx->mEgl);
      }
      break;

    // the app's activity is being destroyed,
    // and waiting for the app thread to clean up and exit before proceeding.
    case APP_CMD_DESTROY:
      VRB_LOG("APP_CMD_DESTROY");
      ctx->mWorld->ShutdownJava();
      break;

    default:
      break;
  }
}

void
android_main(android_app *aAppState) {

  if (!ALooper_forThread()) {
    ALooper_prepare(0);
  }

  // Attach JNI thread
  JNIEnv *jniEnv;
  (*aAppState->activity->vm).AttachCurrentThread(&jniEnv, NULL);

  // Create Browser context
  auto appContext = std::make_shared<AppContext>();
  appContext->mQueue = vrb::RunnableQueue::Create(aAppState->activity->vm);
  appContext->mWorld = BrowserWorld::Create();

  // Create device delegate
  appContext->mDevice = DeviceDelegateOculusVR::Create(appContext->mWorld->GetWeakContext(), aAppState);
  appContext->mWorld->RegisterDeviceDelegate(appContext->mDevice);

  // Initialize java
  auto assetManager = GetAssetManager(jniEnv, aAppState->activity->clazz);
  appContext->mWorld->InitializeJava(jniEnv, aAppState->activity->clazz, assetManager);
  jniEnv->DeleteLocalRef(assetManager);

  // Set up activity & SurfaceView life cycle callbacks
  aAppState->userData = appContext.get();
  aAppState->onAppCmd = CommandCallback;

  // Main render loop
  while (true) {
    int events;
    android_poll_source *pSource;

    // Loop until all events are read
    // If the activity is paused use a blocking call to read events.
    while (ALooper_pollAll(appContext->mWorld->IsPaused() ? -1 : 0,
                           NULL,
                           &events,
                           (void **) &pSource) >= 0) {
      // Process event.
      if (pSource) {
        pSource->process(aAppState, pSource);
      }

      // Check if we are exiting.
      if (aAppState->destroyRequested != 0) {
        appContext->mWorld->ShutdownGL();
        appContext->mEgl->Destroy();
        aAppState->activity->vm->DetachCurrentThread();
        exit(0);
      }
    }
    if (appContext->mEgl) {
      appContext->mEgl->MakeCurrent();
    }
    appContext->mQueue->ProcessRunnables();
    if (!appContext->mWorld->IsPaused() && appContext->mDevice->IsInVRMode()) {
      VRB_CHECK(glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT));
      appContext->mWorld->Draw();
    }
  }
}
