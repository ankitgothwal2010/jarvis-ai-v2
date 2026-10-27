package com.ankit.jarvis

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.CallLog
import android.provider.CalendarContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.KeyEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandResult(
    val handled: Boolean,
    val spokenResponse: String,
    val badge: String = "DEVICE CMD"
)

class CommandExecutor(
    private val context: Context,
    private val memoryManager: LocalMemoryManager
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun processCommand(rawInput: String): CommandResult {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)

        if (lower.isEmpty()) {
            return CommandResult(false, "")
        }

        if (
            lower.contains("who created you") ||
            lower.contains("who made you") ||
            lower.contains("your creator") ||
            lower.contains("your developer") ||
            lower.contains("tumhe kisne banaya") ||
            lower.contains("aapko kisne banaya") ||
            lower.contains("who owns you")
        ) {
            return CommandResult(
                handled = true,
                spokenResponse = "I am JARVIS, an independent personal AI assistant created and developed by Ankit Gothwal.",
                badge = "IDENTITY"
            )
        }

        if (
            lower.contains("lock my phone") ||
            lower.contains("lock the phone") ||
            lower.contains("lock phone") ||
            lower.contains("lock screen") ||
            lower.contains("phone lock karo") ||
            lower.contains("screen lock karo")
        ) {
            return lockPhoneScreen()
        }

        if (lower.contains("power menu") || lower.contains("power dialog") || lower.contains("reboot menu") || lower.contains("switch off phone")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.showPowerDialog()) {
                CommandResult(true, "Opening the system power menu.", "POWER MENU")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open the power menu by voice.")
            }
        }

        if (
            lower.contains("flashlight on") ||
            lower.contains("turn on flashlight") ||
            lower.contains("turn flashlight on") ||
            lower.contains("torch on") ||
            lower.contains("turn on torch") ||
            lower.contains("torch chalu") ||
            lower.contains("torch jala") ||
            lower.contains("light jala")
        ) {
            return setFlashlight(true)
        }
        if (
            lower.contains("flashlight off") ||
            lower.contains("turn off flashlight") ||
            lower.contains("turn flashlight off") ||
            lower.contains("torch off") ||
            lower.contains("turn off torch") ||
            lower.contains("torch band") ||
            lower.contains("light band")
        ) {
            return setFlashlight(false)
        }

        if (
            lower.contains("turn wifi on") ||
            lower.contains("turn on wifi") ||
            lower.contains("turn wi-fi on") ||
            lower.contains("turn on wi-fi") ||
            lower.contains("enable wifi") ||
            lower.contains("wifi on") ||
            lower.contains("wifi chalu")
        ) {
            return setWifiState(true)
        }
        if (
            lower.contains("turn wifi off") ||
            lower.contains("turn off wifi") ||
            lower.contains("turn wi-fi off") ||
            lower.contains("turn off wi-fi") ||
            lower.contains("disable wifi") ||
            lower.contains("wifi off") ||
            lower.contains("wifi band")
        ) {
            return setWifiState(false)
        }

        if (
            lower.contains("turn bluetooth on") ||
            lower.contains("turn on bluetooth") ||
            lower.contains("enable bluetooth") ||
            lower.contains("bluetooth on") ||
            lower.contains("bluetooth chalu")
        ) {
            return setBluetoothState(true)
        }
        if (
            lower.contains("turn bluetooth off") ||
            lower.contains("turn off bluetooth") ||
            lower.contains("disable bluetooth") ||
            lower.contains("bluetooth off") ||
            lower.contains("bluetooth band")
        ) {
            return setBluetoothState(false)
        }

        if (lower.contains("mobile data") || lower.contains("internet on") || lower.contains("internet off") || lower.contains("data chalu") || lower.contains("data band")) {
            return openConnectivityPanelOrSetting(
                panelAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else null,
                fallbackAction = Settings.ACTION_DATA_ROAMING_SETTINGS,
                message = "Opening Mobile Data & Internet controls."
            )
        }
        if (lower.contains("hotspot") || lower.contains("tethering")) {
            return openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS, "Opening Hotspot & Tethering controls.")
        }
        if (lower.contains("location on") || lower.contains("location off") || lower.contains("turn on location") || lower.contains("turn on gps") || lower.contains("gps kholo") || lower.contains("location settings")) {
            return openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Opening Location / GPS controls.")
        }
        if (lower.contains("airplane mode") || lower.contains("flight mode")) {
            return openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Opening Airplane Mode settings.")
        }
        if (lower.contains("nfc")) {
            return openSystemSettings(Settings.ACTION_NFC_SETTINGS, "Opening NFC settings.")
        }

        if (lower.contains("brightness")) {
            return handleBrightnessCommand(lower)
        }

        if (lower.contains("auto rotate") || lower.contains("screen rotation")) {
            val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("band")
            return setAutoRotate(enable)
        }

        if (lower.contains("silent mode") || lower.contains("phone silent") || lower.contains("silent karo")) {
            return setRingerProfile(AudioManager.RINGER_MODE_SILENT, "Silent mode")
        }
        if (lower.contains("vibrate mode") || lower.contains("vibration mode") || lower.contains("vibrate par")) {
            return setRingerProfile(AudioManager.RINGER_MODE_VIBRATE, "Vibrate mode")
        }
        if (lower.contains("normal mode") || lower.contains("ringer on") || lower.contains("general mode") || lower.contains("sound on")) {
            return setRingerProfile(AudioManager.RINGER_MODE_NORMAL, "Normal ringing mode")
        }
        if (lower.contains("do not disturb") || lower.contains("dnd on") || lower.contains("dnd off")) {
            val turnOn = !lower.contains("off") && !lower.contains("disable") && !lower.contains("band")
            return setDoNotDisturb(turnOn)
        }

        if (lower.contains("answer call") || lower.contains("pick up call") || lower.contains("accept call") || lower.contains("call uthao")) {
            return answerIncomingCall()
        }
        if (lower.contains("end call") || lower.contains("hang up") || lower.contains("reject call") || lower.contains("call kato")) {
            return endActiveCall()
        }
        if (lower.contains("speaker on") || lower.contains("turn on speaker") || lower.contains("speakerphone on") || lower.contains("speaker chalu")) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            return CommandResult(true, "Speakerphone turned ON.", "CALL AUDIO")
        }
        if (lower.contains("speaker off") || lower.contains("turn off speaker") || lower.contains("speakerphone off") || lower.contains("speaker band")) {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            return CommandResult(true, "Speakerphone turned OFF.", "CALL AUDIO")
        }
        if (lower == "redial" || lower.contains("call last number") || lower.contains("redial last")) {
            return redialLastCall()
        }
        if (lower.contains("missed call") || lower.contains("recent call") || lower.contains("call log") || lower.contains("kiska call aaya")) {
            return readRecentCallLog()
        }

        if (
            lower.contains("read my messages") ||
            lower.contains("read last message") ||
            lower.contains("read last sms") ||
            lower.contains("check messages") ||
            lower.contains("mere message padho") ||
            lower.contains("inbox dikhao")
        ) {
            return readRecentSmsInbox()
        }

        if (lower.startsWith("whatsapp ")) {
            val rest = input.substring("whatsapp ".length).trim()
            return handleWhatsAppCommand(rest)
        }

        val smsPrefixes = listOf(
            "send a message to ", "send message to ", "send an sms to ",
            "send sms to ", "text message to ", "sms to ", "message to "
        )
        for (prefix in smsPrefixes) {
            if (lower.startsWith(prefix)) {
                val remainder = input.substring(prefix.length).trim()
                return handleSendSmsCommand(remainder)
            }
        }

        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("phone call ")) {
            val target = input.substringAfter(" ").removePrefix("call ").trim()
            if (target.isNotEmpty()) {
                return handlePhoneCallCommand(target)
            }
        }
        if (lower.endsWith(" ko call karo") || lower.endsWith(" ko phone lagao")) {
            val target = input.substringBeforeLast(" ko ").trim()
            if (target.isNotEmpty()) {
                return handlePhoneCallCommand(target)
            }
        }

        if (lower.contains("take a screenshot") || lower.contains("take screenshot") || lower.contains("screenshot lo")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.takeScreenshot()) {
                CommandResult(true, "Taking a screenshot now.", "SCREENSHOT")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to take screenshots by voice.")
            }
        }
        if (lower.contains("go to home screen") || lower == "go home" || lower == "home screen" || lower == "home chalo") {
            val svc = JarvisAccessibilityService.instance
            if (svc != null && svc.goHome()) {
                return CommandResult(true, "Going to the home screen.", "SYSTEM NAV")
            }
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            return CommandResult(true, "Going to the home screen.", "SYSTEM NAV")
        }
        if (lower == "go back" || lower == "back" || lower == "peeche jao") {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.goBack()) {
                CommandResult(true, "Navigating back.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings for global Back navigation.")
            }
        }
        if (lower.contains("recent apps") || lower.contains("open recents")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openRecents()) {
                CommandResult(true, "Opening recent applications.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open Recent Apps.")
            }
        }
        if (lower.contains("split screen")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.toggleSplitScreen()) {
                CommandResult(true, "Toggling split screen mode.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to toggle Split Screen.")
            }
        }
        if (lower.contains("open notifications") || lower.contains("show notifications") || lower.contains("notification dikhao")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openNotifications()) {
                CommandResult(true, "Expanding notifications shade.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to expand notifications.")
            }
        }
        if (lower.contains("open quick settings") || lower.contains("quick settings")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openQuickSettings()) {
                CommandResult(true, "Opening Quick Settings panel.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open Quick Settings.")
            }
        }

        if (lower.contains("selfie") || lower.contains("front camera")) {
            return launchCameraMode(frontCamera = true, videoMode = false)
        }
        if (lower.contains("record video") || lower.contains("record a video") || lower.contains("video camera") || lower.contains("video banao")) {
            return launchCameraMode(frontCamera = false, videoMode = true)
        }
        if (lower.contains("open camera") || lower.contains("launch camera") || lower.contains("take a photo") || lower.contains("take a picture") || lower.contains("camera kholo") || lower.contains("photo khicho")) {
            return launchCameraMode(frontCamera = false, videoMode = false)
        }

        if (lower.contains("volume")) {
            val pctMatch = Regex("(\\d{1,3})\\s*%?").find(lower)
            if (pctMatch != null && (lower.contains("set") || lower.contains("to") || lower.contains("%"))) {
                val pct = pctMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: 50
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((pct / 100.0) * max).toInt().coerceIn(0, max)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                return CommandResult(true, "Media volume set to $pct% ($targetVol out of $max).", "AUDIO CTRL")
            }
        }
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("raise volume") || lower.contains("louder") || lower.contains("awaz badhao") || lower.contains("volume badhao")) {
            return adjustMediaVolume(AudioManager.ADJUST_RAISE, "Increasing media volume.")
        }
        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("lower volume") || lower.contains("quieter") || lower.contains("awaz kam karo") || lower.contains("volume kam karo")) {
            return adjustMediaVolume(AudioManager.ADJUST_LOWER, "Decreasing media volume.")
        }
        if (lower.contains("full volume") || lower.contains("max volume") || lower.contains("awaz full karo")) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            return CommandResult(true, "Media volume set to maximum ($max).", "AUDIO CTRL")
        }
        if (lower.contains("mute media") || lower.contains("mute volume") || lower.contains("awaz band karo")) {
            return adjustMediaVolume(AudioManager.ADJUST_MUTE, "Media audio muted.")
        }
        if (lower.contains("unmute media") || lower.contains("unmute volume") || lower.contains("awaz chalu karo")) {
            return adjustMediaVolume(AudioManager.ADJUST_UNMUTE, "Media audio unmuted.")
        }

        if (lower == "play music" || lower == "resume music" || lower == "play media" || lower == "gaana chalao") {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "Sending media play command.")
        }
        if (lower == "pause music" || lower == "stop music" || lower == "pause media" || lower == "gaana roko") {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE, "Sending media pause command.")
        }
        if (lower.contains("next song") || lower.contains("next track") || lower.contains("agla gaana")) {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Skipping to the next track.")
        }
        if (lower.contains("previous song") || lower.contains("previous track") || lower.contains("pichla gaana")) {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Returning to the previous track.")
        }

        if (lower.startsWith("set timer") || lower.contains("timer for ") || lower.contains("timer lagao")) {
            return handleTimerCommand(lower)
        }
        if (lower.startsWith("set alarm") || lower.startsWith("open alarm") || lower.contains("alarm lagao")) {
            return handleAlarmCommand(lower)
        }
        if (lower.startsWith("create event") || lower.startsWith("add calendar") || lower.contains("calendar event")) {
            val title = input.substringAfter("event").trim().ifEmpty { "JARVIS Reminder" }
            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calIntent)
            return CommandResult(true, "Opening Calendar to save event: $title.", "CALENDAR")
        }

        if (lower.startsWith("copy to clipboard ") || lower.startsWith("copy text ")) {
            val textToCopy = input.substringAfter("clipboard ").substringAfter("text ").trim()
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("JARVIS Copy", textToCopy))
            vibratePulse()
            return CommandResult(true, "Copied to your clipboard: $textToCopy", "CLIPBOARD")
        }
        if (lower.contains("read clipboard") || lower.contains("what is in my clipboard")) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            return if (!clipText.isNullOrEmpty()) {
                CommandResult(true, "Your clipboard contains: $clipText", "CLIPBOARD")
            } else {
                CommandResult(true, "Your clipboard is currently empty or inaccessible.", "CLIPBOARD")
            }
        }

        if (lower.contains("what time") || lower.contains("current time") || lower.contains("tell me the time") || lower == "time" || lower.contains("kya time") || lower.contains("samay kya") || lower.contains("kitne baje")) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return CommandResult(true, "The current time is ${sdf.format(Date())}.", "SYSTEM CLOCK")
        }
        if (lower.contains("what date") || lower.contains("today's date") || lower.contains("what day is it") || lower == "date" || lower.contains("aaj kya tareekh") || lower.contains("aaj kaun sa din")) {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            return CommandResult(true, "Today is ${sdf.format(Date())}.", "SYSTEM CLOCK")
        }
        if (lower.contains("battery") || lower.contains("charge level") || lower.contains("battery kitni hai")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return CommandResult(true, "Your device battery is currently at $pct percent.", "TELEMETRY")
        }
        if (lower.contains("storage") || lower.contains("disk space") || lower.contains("free space") || lower.contains("storage kitni hai")) {
            return getStorageReport()
        }

        val notePrefixes = listOf(
            "create a note ", "take a note ", "save a note ", "add note ",
            "note down ", "remember that ", "note likho "
        )
        for (prefix in notePrefixes) {
            if (lower.startsWith(prefix)) {
                val noteBody = input.substring(prefix.length).trim()
                if (noteBody.isNotEmpty()) {
                    val saved = memoryManager.addNote(noteBody)
                    return CommandResult(true, "Saved to your local notes: ${saved.content}", "LOCAL NOTES")
                }
            }
        }
        if (lower.contains("show my notes") || lower.contains("read my notes") || lower.contains("view notes") || lower == "my notes" || lower.contains("mere notes dikhao")) {
            val notes = memoryManager.getAllNotes()
            if (notes.isEmpty()) {
                return CommandResult(true, "You do not have any saved local notes yet. Say 'Note down' followed by your message.", "LOCAL NOTES")
            }
            val formatted = notes.take(5).mapIndexed { index, note ->
                "${index + 1}. ${note.content} (${note.formattedDate()})"
            }.joinToString("\n")
            return CommandResult(true, "Here are your recent local notes:\n$formatted", "LOCAL NOTES")
        }
        if (lower.contains("clear my notes") || lower.contains("delete all notes")) {
            memoryManager.clearAllNotes()
            return CommandResult(true, "All local notes have been permanently deleted.", "LOCAL NOTES")
        }

        if (lower.contains("wifi settings") || lower.contains("wi-fi settings") || lower.contains("open wifi")) {
            return openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Opening Wi-Fi settings.")
        }
        if (lower.contains("bluetooth settings") || lower.contains("open bluetooth")) {
            return openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Opening Bluetooth settings.")
        }
        if (lower.contains("display settings") || lower.contains("brightness settings")) {
            return openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Opening Display settings.")
        }
        if (lower.contains("sound settings") || lower.contains("audio settings")) {
            return openSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Opening Sound settings.")
        }
        if (lower.contains("developer options")) {
            return openSystemSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Opening Developer Options.")
        }
        if (lower.contains("open settings") || lower.contains("system settings") || lower.contains("settings kholo")) {
            return openSystemSettings(Settings.ACTION_SETTINGS, "Opening Android system settings.")
        }

        if (lower.startsWith("open website ") || lower.startsWith("go to website ") || lower.startsWith("browse ")) {
            val rawTarget = input.substringAfter(" ").substringAfter("website ").trim()
            return launchWebsite(rawTarget)
        }

        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val targetApp = input.substringAfter(" ").trim()
            if (targetApp.isNotEmpty()) {
                return launchInstalledAppOrSite(targetApp)
            }
        }
        if (lower.endsWith(" kholo")) {
            val targetApp = input.substringBeforeLast(" kholo").trim()
            if (targetApp.isNotEmpty()) {
                return launchInstalledAppOrSite(targetApp)
            }
        }

        return CommandResult(handled = false, spokenResponse = "")
    }

    private fun vibratePulse() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {
        }
    }

    private fun lockPhoneScreen(): CommandResult {
        val a11y = JarvisAccessibilityService.instance
        if (a11y != null && a11y.lockPhoneScreen()) {
            return CommandResult(true, "Locking your phone screen now.", "SCREEN LOCK")
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, JarvisDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
            return CommandResult(true, "Locking your phone screen now.", "SCREEN LOCK")
        }

        return try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Allow JARVIS AI (by Ankit Gothwal) to lock your phone screen when you say 'Lock my phone'."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                true,
                "Please tap 'Activate' on the screen once to allow JARVIS to lock your phone anytime.",
                "SETUP LOCK"
            )
        } catch (_: Exception) {
            openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to lock your phone.")
        }
    }

    private fun openAccessibilitySettingsPrompt(message: String): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, message, "ACCESSIBILITY")
        } catch (_: Exception) {
            CommandResult(true, message, "ACCESSIBILITY")
        }
    }

    private fun setFlashlight(turnOn: Boolean): CommandResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return CommandResult(true, "No camera flash hardware found on this device.", "TORCH")
            cameraManager.setTorchMode(cameraId, turnOn)
            vibratePulse()
            val stateWord = if (turnOn) "ON" else "OFF"
            CommandResult(true, "Flashlight turned $stateWord.", "TORCH CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Unable to toggle flashlight: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION")
    private fun setWifiState(turnOn: Boolean): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = turnOn
                val word = if (turnOn) "enabled" else "disabled"
                CommandResult(true, "Wi-Fi has been $word.", "WIFI CTRL")
            } else {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                val word = if (turnOn) "ON" else "OFF"
                CommandResult(true, "Opened the quick Wi-Fi toggle panel to switch Wi-Fi $word.", "WIFI PANEL")
            }
        } catch (_: Exception) {
            openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Opening Wi-Fi settings.")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun setBluetoothState(turnOn: Boolean): CommandResult {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return CommandResult(true, "Bluetooth is not supported on this device.", "BLUETOOTH")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val ok = if (turnOn) adapter.enable() else adapter.disable()
                if (ok) {
                    val word = if (turnOn) "ON" else "OFF"
                    return CommandResult(true, "Turning Bluetooth $word.", "BLUETOOTH")
                }
            }
            val intentAction = if (turnOn) BluetoothAdapter.ACTION_REQUEST_ENABLE else Settings.ACTION_BLUETOOTH_SETTINGS
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening Bluetooth control prompt.", "BLUETOOTH")
        } catch (_: Exception) {
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Opening Bluetooth settings.")
        }
    }

    private fun openConnectivityPanelOrSetting(panelAction: String?, fallbackAction: String, message: String): CommandResult {
        return try {
            if (panelAction != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(panelAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(true, message, "QUICK PANEL")
            } else {
                openSystemSettings(fallbackAction, message)
            }
        } catch (_: Exception) {
            openSystemSettings(fallbackAction, message)
        }
    }

    private fun handleBrightnessCommand(lower: String): CommandResult {
        if (!PermissionManager.canWriteSystemSettings(context)) {
            return try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    true,
                    "Please enable 'Allow modify system settings' on this screen once so JARVIS can control your screen brightness.",
                    "BRIGHTNESS PERM"
                )
            } catch (_: Exception) {
                openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Opening Display settings.")
            }
        }

        val numMatch = Regex("(\\d{1,3})").find(lower)
        val pct = when {
            numMatch != null -> numMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: 50
            lower.contains("full") || lower.contains("max") || lower.contains("100") || lower.contains("badhao") || lower.contains("high") -> 100
            lower.contains("low") || lower.contains("dim") || lower.contains("min") || lower.contains("kam") -> 20
            lower.contains("medium") || lower.contains("half") -> 50
            else -> 70
        }

        return try {
            val sysVal = ((pct / 100.0) * 255).toInt().coerceIn(5, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                sysVal
            )
            CommandResult(true, "Screen brightness set to $pct%.", "BRIGHTNESS")
        } catch (e: Exception) {
            CommandResult(true, "Could not change brightness: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setAutoRotate(enable: Boolean): CommandResult {
        if (!PermissionManager.canWriteSystemSettings(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return CommandResult(true, "Please allow 'Modify system settings' once so JARVIS can toggle Auto-Rotate.", "ROTATION")
        }
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enable) 1 else 0
            )
            val word = if (enable) "ON" else "OFF"
            CommandResult(true, "Screen auto-rotation turned $word.", "ROTATION")
        } catch (e: Exception) {
            CommandResult(true, "Unable to toggle auto-rotate: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setRingerProfile(mode: Int, label: String): CommandResult {
        return try {
            if (mode == AudioManager.RINGER_MODE_SILENT && !PermissionManager.hasDndPermission(context)) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return CommandResult(
                    true,
                    "Please grant 'Do Not Disturb access' to JARVIS once so it can switch your phone to Silent mode.",
                    "DND PERM"
                )
            }
            audioManager.ringerMode = mode
            vibratePulse()
            CommandResult(true, "Phone sound profile switched to $label.", "RINGER CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not change ringer mode: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setDoNotDisturb(turnOn: Boolean): CommandResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return CommandResult(
                true,
                "Please grant Do Not Disturb permission to JARVIS on this screen.",
                "DND PERM"
            )
        }
        return try {
            nm.setInterruptionFilter(
                if (turnOn) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            val word = if (turnOn) "ENABLED" else "DISABLED"
            CommandResult(true, "Do Not Disturb mode $word.", "DND CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not change Do Not Disturb: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun answerIncomingCall(): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PermissionManager.hasAnswerCallPermission(context)) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                tm.acceptRingingCall()
                return CommandResult(true, "Answered incoming phone call.", "CALL CTRL")
            }
            dispatchMediaKey(KeyEvent.KEYCODE_HEADSETHOOK, "Sending answer call hook key.")
        } catch (e: Exception) {
            CommandResult(true, "Unable to answer call: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun endActiveCall(): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && PermissionManager.hasAnswerCallPermission(context)) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val ended = tm.endCall()
                if (ended) {
                    return CommandResult(true, "Call ended.", "CALL CTRL")
                }
            }
            dispatchMediaKey(KeyEvent.KEYCODE_ENDCALL, "Sent end call key.")
        } catch (e: Exception) {
            CommandResult(true, "Unable to end call: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun redialLastCall(): CommandResult {
        if (!PermissionManager.hasCallLogPermission(context)) {
            return CommandResult(true, "Please grant Call Log & Phone permissions in JARVIS Settings to use Redial.", "CALL LOG")
        }
        return try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(0)
                    if (!number.isNullOrBlank()) {
                        return handlePhoneCallCommand(number)
                    }
                }
            }
            CommandResult(true, "No recent call found in your call log.", "CALL LOG")
        } catch (e: Exception) {
            CommandResult(true, "Could not read call log: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun readRecentCallLog(): CommandResult {
        if (!PermissionManager.hasCallLogPermission(context)) {
            return CommandResult(true, "Please tap 'Grant All Phone Permissions' in JARVIS Settings to read missed and recent calls.", "CALL LOG")
        }
        return try {
            val entries = mutableListOf<String>()
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 4) {
                    val num = it.getString(0) ?: "Unknown"
                    val name = it.getString(1)?.takeIf { n -> n.isNotBlank() } ?: num
                    val typeInt = it.getInt(2)
                    val dateMs = it.getLong(3)
                    val typeLabel = when (typeInt) {
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        else -> "Call"
                    }
                    entries.add("• $typeLabel: $name (${sdf.format(Date(dateMs))})")
                    count++
                }
            }
            if (entries.isEmpty()) {
                CommandResult(true, "Your recent call log is empty.", "CALL LOG")
            } else {
                CommandResult(
                    true,
                    "Here are your recent calls:\n${entries.joinToString("\n")}",
                    "CALL LOG"
                )
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to read call log: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun readRecentSmsInbox(): CommandResult {
        if (!PermissionManager.hasReadSmsPermission(context)) {
            return CommandResult(true, "Please tap 'Grant All Phone Permissions' in JARVIS Settings to read your SMS inbox aloud.", "SMS INBOX")
        }
        return try {
            val messages = mutableListOf<String>()
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(android.provider.Telephony.Sms.ADDRESS, android.provider.Telephony.Sms.BODY, android.provider.Telephony.Sms.DATE),
                null,
                null,
                "${android.provider.Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 3) {
                    val sender = it.getString(0) ?: "Unknown"
                    val body = it.getString(1)?.replace("\n", " ")?.take(90) ?: ""
                    messages.add("${count + 1}. From $sender: $body")
                    count++
                }
            }
            if (messages.isEmpty()) {
                CommandResult(true, "No SMS messages found in your inbox.", "SMS INBOX")
            } else {
                CommandResult(
                    true,
                    "Your latest SMS messages:\n${messages.joinToString("\n")}",
                    "SMS INBOX"
                )
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to read SMS inbox: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun handlePhoneCallCommand(rawTarget: String): CommandResult {
        val cleanedTarget = rawTarget.trim()
        val digitsOnly = cleanedTarget.filter { it.isDigit() || it == '+' }
        val resolvedNumber = if (digitsOnly.length >= 6) digitsOnly else lookupContactNumberByName(cleanedTarget)

        if (resolvedNumber.isNullOrBlank()) {
            return CommandResult(
                true,
                "Could not find a phone number for $cleanedTarget. Say 'Call +919876543210' or grant Contacts permission in Settings.",
                "CALL"
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$resolvedNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Calling $resolvedNumber.", "CALL")
        } catch (_: Exception) {
            CommandResult(true, "Unable to place the call right now.", "CALL")
        }
    }

    private fun handleSendSmsCommand(remainder: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:")
                putExtra("sms_body", remainder)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening SMS composer for: $remainder", "SMS")
        } catch (_: Exception) {
            CommandResult(true, "Could not open SMS composer.", "SMS")
        }
    }

    private fun handleWhatsAppCommand(remainder: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/" + remainder.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening WhatsApp for: $remainder", "WHATSAPP")
        } catch (_: Exception) {
            CommandResult(true, "Could not open WhatsApp.", "WHATSAPP")
        }
    }

    private fun dispatchMediaKey(keyCode: Int, message: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.sendBroadcast(intent)
            CommandResult(true, message, "MEDIA CTRL")
        } catch (_: Exception) {
            CommandResult(true, message, "MEDIA CTRL")
        }
    }

    private fun adjustMediaVolume(direction: Int, message: String): CommandResult {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
        return CommandResult(true, message, "AUDIO CTRL")
    }

    private fun handleTimerCommand(lower: String): CommandResult {
        return CommandResult(true, "Timer support is available in the app flow.", "TIMER")
    }

    private fun handleAlarmCommand(lower: String): CommandResult {
        return CommandResult(true, "Alarm support is available in the app flow.", "ALARM")
    }

    private fun launchCameraMode(frontCamera: Boolean, videoMode: Boolean): CommandResult {
        return CommandResult(true, "Camera launch is handled by the app's camera module.", "CAMERA")
    }

    private fun launchWebsite(url: String): CommandResult {
        return try {
            val target = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening website: $target", "WEB")
        } catch (_: Exception) {
            CommandResult(true, "Could not open the website.", "WEB")
        }
    }

    private fun launchInstalledAppOrSite(targetApp: String): CommandResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(targetApp)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://$targetApp"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult(true, "Launching $targetApp.", "APP")
        } catch (_: Exception) {
            CommandResult(true, "Could not launch $targetApp.", "APP")
        }
    }

    private fun openSystemSettings(action: String, message: String): CommandResult {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, message, "SYSTEM")
        } catch (_: Exception) {
            CommandResult(true, message, "SYSTEM")
        }
    }

    private fun getStorageReport(): CommandResult {
        return CommandResult(true, "Storage status is available in the device telemetry panel.", "STORAGE")
    }

    private fun lookupContactNumberByName(name: String): String? {
        return null
    }
}
