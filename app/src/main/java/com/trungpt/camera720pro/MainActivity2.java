//package com.trungpt.camera720pro;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.Button;
//import com.aviary.android.feather.library.Constants;
//import com.aviary.android.feather.sdk.FeatherActivity;
//
//import java.io.File;
//
//
//public class MainActivity2 extends Activity
//{
//
//    Button btTest;
//    Button btPickFromGallery;
//    Uri fileUri;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        btTest = (Button) findViewById(R.id.test);
//        btPickFromGallery = (Button) findViewById(R.id.btPickFromGallery);
//        btTest.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
//                fileUri = Utils.getOutputMediaFileUri(Utils.MEDIA_TYPE_IMAGE);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
//                startActivityForResult(intent, Const.REQUEST_CODE_CAMERA);
//            }
//        });
//
//        btPickFromGallery.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                pickFromGallery();
//            }
//        });
//
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data)
//    {
//        if (resultCode == RESULT_OK)
//        {
//            switch (requestCode)
//            {
//                case 1:
//                    // output image path
//                    Uri mImageUri = data.getData();
//                    Bundle extra = data.getExtras();
//                    if (null != extra)
//                    {
//                        // image has been changed by the user?
//                        boolean changed = extra.getBoolean(Constants.EXTRA_OUT_BITMAP_CHANGED);
//                    }
//                    break;
//                case Const.REQUEST_CODE_CAMERA:
//                    Intent newIntent = new Intent(this, FeatherActivity.class);
//                    newIntent.setData(fileUri);
//                    newIntent.putExtra(Constants.EXTRA_IN_API_KEY_SECRET, Const.api_secret);
//                    startActivityForResult(newIntent, 1);
//                    break;
//            }
//        }
//    }
//
//    private void pickFromGallery()
//    {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/*");
//        Intent chooser = Intent.createChooser(intent, "Choose a Picture");
//        startActivityForResult(chooser, Const.ACTION_REQUEST_GALLERY);
//    }
//
//    @SuppressWarnings("deprecation")
//    private void startFeather( Uri uri ) {
//
//        // first check the external storage availability
//        if ( !isExternalStorageAvailable() ) {
//            showDialog( EXTERNAL_STORAGE_UNAVAILABLE );
//            return;
//        }
//
//        // create a temporary file where to store the resulting image
//        File file = getNextFileName();
//
//
//        if ( null != file ) {
//            mOutputFilePath = file.getAbsolutePath();
//        } else {
//            new AlertDialog.Builder( this ).setTitle( android.R.string.dialog_alert_title ).setMessage( "Failed to create a new File" )
//                    .show();
//            return;
//        }
//
//        // Create the intent needed to start feather
//        Intent newIntent = new Intent( this, FeatherActivity.class );
//
//        // === INPUT IMAGE URI (MANDATORY) ===
//        // Set the source image uri
//        newIntent.setData( uri );
//
//        // === API KEY SECRET (MANDATORY) ====
//        // You must pass your Aviary key secret
//        newIntent.putExtra( Constants.EXTRA_IN_API_KEY_SECRET, API_SECRET );
//
//        // <pcn:true>
//        // == BILLING public key (MANDATORY if you're a PCN partner) ===
//        // You can find your app's base64-encoded
//        // public key in your application's page on Google Play Developer Console.
//        // Note that this is NOT your "developer public key".
//        // For more info: http://developer.android.com/training/in-app-billing/preparing-iab-app.html
//        newIntent.putExtra( Constants.EXTRA_IN_BILLING_PUBLIC_KEY, BILLING_API );
//        // </pcn:true>
//
//
//        // === OUTPUT (OPTIONAL/RECOMMENDED)====
//        // Pass the uri of the destination image file.
//        // This will be the same uri you will receive in the onActivityResult
//        newIntent.putExtra( Constants.EXTRA_OUTPUT, Uri.parse( "file://" + mOutputFilePath ) );
//
//        // === OUTPUT FORMAT (OPTIONAL) ===
//        // Format of the destination image
//        newIntent.putExtra( Constants.EXTRA_OUTPUT_FORMAT, Bitmap.CompressFormat.JPEG.name() );
//
//        // === OUTPUT QUALITY (OPTIONAL) ===
//        // Output format quality (jpeg only)
//        newIntent.putExtra( Constants.EXTRA_OUTPUT_QUALITY, 90 );
//
//
//        // === WHITE LABEL (OPTIONAL/PREMIUM ONLY) ===
//        // If you want to hide the 'feedback' button and the 'aviary' logo
//        // pass this intent-extra
//        // Note that you need to have the 'whitelabel' permissions enabled in order
//        // to use this extra
//        newIntent.putExtra( Constants.EXTRA_WHITELABEL, true );
//
//        // === HIRES (OPTIONAL/PREMIUM ONLY) ===
//        // If you want hires image processing pass this intent-extra with the maximum size
//        // megapixels you want the saved image to be
//        // Note that you need to have the 'hires' permissions enabled in order to use this extra
//        newIntent.putExtra( Constants.EXTRA_IN_HIRES_MEGAPIXELS, MegaPixels.Mp5.ordinal() );
//
//        // == TOOLS LIST ===
//        // Optional
//        // You can force feather to display only some tools ( see FilterLoaderFactory#Filters )
//        // you can omit this if you just want to display the default tools
//
//
////		 newIntent.putExtra( "tools-list", new String[] {
////			 FilterLoaderFactory.Filters.ENHANCE.name(),
////			 FilterLoaderFactory.Filters.EFFECTS.name(),
////			 FilterLoaderFactory.Filters.STICKERS.name(),
////			 FilterLoaderFactory.Filters.CROP.name(),
////			 FilterLoaderFactory.Filters.BLUR.name(),
////			 FilterLoaderFactory.Filters.BRIGHTNESS.name(),
////			 FilterLoaderFactory.Filters.CONTRAST.name(),
////			 FilterLoaderFactory.Filters.SATURATION.name(),
////			 FilterLoaderFactory.Filters.SHARPNESS.name(),
////			 FilterLoaderFactory.Filters.SPLASH.name(),
////			 FilterLoaderFactory.Filters.DRAW.name(),
////			 FilterLoaderFactory.Filters.TEXT.name(),
////			 FilterLoaderFactory.Filters.REDEYE.name(),
////			 FilterLoaderFactory.Filters.WHITEN.name(),
////			 FilterLoaderFactory.Filters.BLEMISH.name(),
////			 FilterLoaderFactory.Filters.MEME.name(),
////		 } );
//
//
//        // === EXIT ALERT (OPTIONAL) ===
//        // You want to hide the exit alert dialog shown when back is pressed
//        // without saving image first
//        // newIntent.putExtra( Constants.EXTRA_HIDE_EXIT_UNSAVE_CONFIRMATION, true );
//
//        // === VIBRATION (OPTIONAL) ===
//        // Some aviary tools use the device vibration in order to give a better experience
//        // to the final user. But if you want to disable this feature, just pass
//        // any value with the key "tools-vibration-disabled" in the calling intent.
//        // This option has been added to version 2.1.5 of the Aviary SDK
//        // newIntent.putExtra( Constants.EXTRA_TOOLS_DISABLE_VIBRATION, true );
//
//        // === NO CHANGES (OPTIONAL) ==
//        // With this extra param you can tell to FeatherActivity how to manage
//        // the press on the Done button even when no real changes were made to
//        // the image.
//        // If the value is true then the image will be always saved, a RESULT_OK
//        // will be returned to your onActivityResult and the result Bundle will
//        // contain an extra value "EXTRA_OUT_BITMAP_CHANGED" indicating if the
//        // image was changed during the session.
//        // If "false" is passed then a RESULT_CANCEL will be sent when an user will
//        // hit the 'Done' button without any modifications ( also the EXTRA_OUT_BITMAP_CHANGED
//        // extra will be sent back to the onActivityResult.
//        // By default this value is true ( even if you omit it )
//        newIntent.putExtra( Constants.EXTRA_IN_SAVE_ON_NO_CHANGES, true );
//
//        // ..and start feather
//        startActivityForResult( newIntent, ACTION_REQUEST_FEATHER );
//    }
//
//    /**
//     * Check the external storage status
//     *
//     * @return
//     */
//    private boolean isExternalStorageAvailable() {
//        String state = Environment.getExternalStorageState();
//        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
//            return true;
//        }
//        return false;
//    }
//}
