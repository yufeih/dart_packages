package com.aboutyou.dart_packages.sign_in_with_apple

import SignInWithAppleConfiguration
import android.content.Intent
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import com.RNAppleAuthentication.SignInWithAppleCallback
import com.RNAppleAuthentication.SignInWithAppleService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar


val TAG = "SignInWithApple"

/** SignInWithApplePlugin */
public class SignInWithApplePlugin: FlutterPlugin, MethodCallHandler, ActivityAware, ActivityResultListener {
  private val CUSTOM_TABS_REQUEST_CODE = 1001;

  private var channel: MethodChannel? = null

  var binding: ActivityPluginBinding? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.aboutyou.dart_packages.sign_in_with_apple")
    channel?.setMethodCallHandler(this);
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel?.setMethodCallHandler(null)
    channel = null
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    var lastAuthorizationRequestResult: Result? = null
    var triggerMainActivityToHideChromeCustomTab : (() -> Unit)? = null

    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "com.aboutyou.dart_packages.sign_in_with_apple")
      channel.setMethodCallHandler(SignInWithApplePlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "isAvailable" -> result.success(true)
      "performAuthorizationRequest" -> {
        val _activity = binding?.activity

        if (_activity == null) {
          result.error("MISSING_ACTIVITY", "Plugin is not attached to an activity", call.arguments)
          return
        }

        val url: String? = call.argument("url")
        if (url == null) {
          result.error("MISSING_ARG", "Missing 'url' argument", call.arguments)
          return
        }

        val redirectUrl: String? = call.argument("redirectUrl")
        if (redirectUrl == null) {
          result.error("MISSING_ARG", "Missing 'redirectUrl' argument", call.arguments)
          return
        }

        val state: String? = call.argument("state")
        if (state == null) {
          result.error("MISSING_ARG", "Missing 'state' argument", call.arguments)
          return
        }

        val callback = object : SignInWithAppleCallback {
          override fun onSignInWithAppleSuccess(
            code: String,
            id_token: String,
            state: String,
            user: String
          ) {
            result.success(hashMapOf<String, Any>(
              "type" to "appleid",
              "authorizationCode" to code,
              "userIdentifier" to user,
              "identityToken" to id_token,
            ))
          }

          override fun onSignInWithAppleCancel() {
            result.success(null);
          }

          override fun onSignInWithAppleFailure(error: Throwable) {
            result.error("SIGNIN_APPLE_FAILURE", error.message, call.arguments)
          }
        }

        val configuration = SignInWithAppleConfiguration(url, redirectUrl, state)
        val fragmentmanager = (_activity as FragmentActivity).supportFragmentManager
        val fragmentTag = "SignInWithAppleButton-SignInWebViewDialogFragment";
        val service = SignInWithAppleService(fragmentmanager, fragmentTag, configuration, callback)
        _activity.runOnUiThread { service.show() }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.binding = binding
    binding.addActivityResultListener(this)
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onDetachedFromActivity() {
    binding?.removeActivityResultListener(this)
    binding = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == CUSTOM_TABS_REQUEST_CODE) {
      val _lastAuthorizationRequestResult = lastAuthorizationRequestResult

      if (_lastAuthorizationRequestResult != null) {
        _lastAuthorizationRequestResult.error("authorization-error/canceled", "The user closed the Custom Tab", null)

        lastAuthorizationRequestResult = null
        triggerMainActivityToHideChromeCustomTab = null
      }
    }

    return false
  }
}
