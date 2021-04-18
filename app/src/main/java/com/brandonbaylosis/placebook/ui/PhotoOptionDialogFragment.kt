package com.brandonbaylosis.placebook.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PhotoOptionDialogFragment : DialogFragment() {
    // 1 Defines interface that must be implemented by parent activity
    interface PhotoOptionDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }
    // 2 Property defined to hold instance of PhotoOptionDialogListener
    private lateinit var listener: PhotoOptionDialogListener
    // 3 Standard onCreateDialog method for a DialogFragment
    override fun onCreateDialog(savedInstanceState: Bundle?):
            Dialog {
        // 4 Listener property is set to parent Activity
        listener = activity as PhotoOptionDialogListener
        // 5 Two possible option indices are initialized to -1
        // These are defined dynamically due to the position of the Gallery
        // and Camera options may change based on device capabilities
        var captureSelectIdx = -1
        var pickSelectIdx = -1
        // 6 Holds the AlertDialog options
        val options = ArrayList<String>()
        // 7 Next calls require Context object, using the activity property
        // of AlertDialog class as context
        // Since the activity property has a getter method and may change between calls,
        // a temporary un-mutable local variable is set and used to prevent
        // compiler errors.
        val context = activity as Context
        // 8 If device has camera capable of capturing images, Camera option's added
        // to options array
        if (canCapture(context)) {
            options.add("Camera")
            // Set to 0 to indicate the Camera option will be at position 0 in option list
            captureSelectIdx = 0
        }
        // 9 If device can pick a device from a gallery, Gallery option's added to array
        if (canPick(context)) {
            options.add("Gallery")
            // pickSelectIdx variable set to 0 if first option, 1 if second
            pickSelectIdx = if (captureSelectIdx == 0) 1 else 0
        }
        // 10 Build AlertDialog using the option list
        // OnClickListener provided to respond to user selection
        return AlertDialog.Builder(context)
                .setTitle("Photo Option")
                .setItems(options.toTypedArray<CharSequence>()) {
                    _, which ->
                    if (which == captureSelectIdx) {
                        // 11 If Camera option selected, calls onCaptureClick
                        listener.onCaptureClick()
                    } else if (which == pickSelectIdx) {
                        // 12 Calls onPickClick if Gallery was selected
                        listener.onPickClick()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
    }
    companion object {
        // 13 Determines if the device can pick an image from a gallery
        fun canPick(context: Context) : Boolean {
            // Creates intent for picking image and checks to see if Intent
            // can be resolved
            val pickIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(
                    context.packageManager) != null)
        }
        // 14 Determines if device has camera to capture a new image, using
        // the same technique as canPick
        fun canCapture(context: Context) : Boolean {
            val captureIntent = Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(
                    context.packageManager) != null)
        }
        // 15 Helper method intended to be used by parent activity
        // when creating a new PhotoOptionDialogFragment
        fun newInstance(context: Context):
                PhotoOptionDialogFragment? {
            // 16 Checks if device can pick from gallery or snap new image
            if (canPick(context) || canCapture(context)) {
                // Creates and returns PhotoOptionDialogFragment if true
                val frag = PhotoOptionDialogFragment()
                return frag
            } else {
                // Returns null otherwise
                return null
            }
            }
        }
    }