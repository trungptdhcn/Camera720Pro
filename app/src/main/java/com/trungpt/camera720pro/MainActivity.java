package com.trungpt.camera720pro;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.aviary.android.feather.cds.billing.util.IabHelper;
import com.aviary.android.feather.cds.billing.util.IabResult;
import com.aviary.android.feather.cds.billing.util.Purchase;
import com.aviary.android.feather.sdk.FeatherActivity;
import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.utils.SDKUtils;
import com.aviary.android.feather.headless.utils.MegaPixels;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.library.utils.DecodeUtils;

public class MainActivity extends Activity {

    /**
     * ========== READ ME FIRST ===========
     * In order to use the Aviary SDK correctly you must first get your own API-KEY from http://aviary.com/android.
     * Then copy your api key inside a metadata tag, inside the application tag, of your AndroidManifest.xml file, like this:
     *
     * 	<meta-data
     * 		android:name="com.aviary.android.feather.v1.API_KEY"
     * 		android:value="xxxxxxxxx" />
     */
    private String mApiKey;

    private static final int ACTION_REQUEST_GALLERY = 99;
    private static final int ACTION_REQUEST_FEATHER = 100;
    private static final int EXTERNAL_STORAGE_UNAVAILABLE = 1;

    // <pcn:true>
    // your app public key, in the google play console
    private static final String BILLING_API = "<your google play public billing key here>";
    // </pcn:true>

    // your aviary secret key
    private static final String API_SECRET = "<your api secret here>";

    public static final String LOG_TAG = "aviary-launcher";

    /** apikey is required http://developers.aviary.com/ */

    /** Folder name on the sdcard where the images will be saved **/
    private static final String FOLDER_NAME = "aviary";

    Button mGalleryButton;
    Button mEditButton;
    ImageView mImage;
    View mImageContainer;
    String mOutputFilePath;
    Uri mImageUri;
    int imageWidth, imageHeight;
    private File mGalleryFolder;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        Log.i( LOG_TAG, "onCreate" );
        super.onCreate( savedInstanceState );

        setContentView( R.layout.main );

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageWidth = (int) ( (float) metrics.widthPixels / 1.5 );
        imageHeight = (int) ( (float) metrics.heightPixels / 1.5 );

        mGalleryButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                pickFromGallery();
            }
        } );

        mEditButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                if ( mImageUri != null ) {
                    startFeather( mImageUri );
                }
            }
        } );

        mImageContainer.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                Uri uri = pickRandomImage();
                if ( uri != null ) {
                    Log.d( LOG_TAG, "image uri: " + uri );
                    loadAsync( uri );
                }
            }
        } );

        mImageContainer.setLongClickable( true );
        mImageContainer.setOnLongClickListener( new OnLongClickListener() {

            @Override
            public boolean onLongClick( View v ) {
                if ( mImageUri != null ) {
                    Log.d( LOG_TAG, "onLongClick: " + v );
                    openContextMenu( v );
                    return true;
                }
                return false;
            }
        } );

        mGalleryFolder = createFolders();
        registerForContextMenu( mImageContainer );

        // pre-load the cds service
        Intent cdsIntent = AviaryIntent.createCdsInitIntent( getBaseContext(), API_SECRET, BILLING_API );

        startService( cdsIntent );

        new ApiKeyReader().execute();

    }

    @Override
    protected void onResume() {
        Log.i( LOG_TAG, "onResume" );
        super.onResume();

        if ( getIntent() != null ) {
            handleIntent( getIntent() );
            setIntent( new Intent() );
        }
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );
        menu.setHeaderTitle( "Menu" );
        menu.add( 0, 0, 0, "Details" );
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        final int order = item.getOrder();
        switch ( order ) {
            case 0:
                showCurrentImageDetails();
                return true;
        }
        return super.onContextItemSelected( item );
    }

    private void setApiKey( String value ) {
        Log.i( LOG_TAG, "api-key: " + value );

        mApiKey = value;

        if( null == value ) {
            String message = SDKUtils.MISSING_APIKEY_MESSAGE;
            new AlertDialog.Builder( this ).setTitle( "API-KEY Missing!" ).setMessage( message ).show();
        } else if ( value.equals( "enter your aviary api key here" ) ) {
            Log.w( LOG_TAG, "You did not set your Aviary API Key in the AndroidManifest!" );
            new AlertDialog.Builder(this)
                    .setTitle("Set your API key!")
                    .setMessage("You did not set your Aviary API key in your AndroidManifest.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

    }

    /**
     * Handle the incoming {@link android.content.Intent}
     */
    private void handleIntent( Intent intent ) {

        String action = intent.getAction();

        if ( null != action ) {

            if ( Intent.ACTION_SEND.equals( action ) ) {

                Bundle extras = intent.getExtras();
                if ( extras != null && extras.containsKey( Intent.EXTRA_STREAM ) ) {
                    Uri uri = (Uri) extras.get( Intent.EXTRA_STREAM );
                    loadAsync( uri );
                }
            } else if ( Intent.ACTION_VIEW.equals( action ) ) {
                Uri data = intent.getData();
                Log.d( LOG_TAG, "data: " + data );
                loadAsync( data );
            }
        }
    }

    /**
     * Load the incoming Image
     *
     * @param uri
     */
    private void loadAsync( final Uri uri ) {
        Log.i( LOG_TAG, "loadAsync: " + uri );

        Drawable toRecycle = mImage.getDrawable();
        if ( toRecycle != null && toRecycle instanceof BitmapDrawable ) {
            if ( ( (BitmapDrawable) mImage.getDrawable() ).getBitmap() != null )
                ( (BitmapDrawable) mImage.getDrawable() ).getBitmap().recycle();
        }
        mImage.setImageDrawable( null );
        mImageUri = null;

        DownloadAsync task = new DownloadAsync();
        task.execute( uri );
    }

    @Override
    protected void onDestroy() {
        Log.i( LOG_TAG, "onDestroy" );
        super.onDestroy();
        mOutputFilePath = null;
    }

    /**
     * Load the image details and pass the result
     * to the {@link ImageInfoActivity} activity
     */
    private void showCurrentImageDetails() {
        if ( null != mImageUri ) {
            ImageInfo info;
            try {
                info = new ImageInfo( this, mImageUri );
            } catch ( IOException e ) {
                e.printStackTrace();
                return;
            }

            if ( null != info ) {
                Intent intent = new Intent( this, ImageInfoActivity.class );
                intent.putExtra( "image-info", info );
                startActivity( intent );
            }
        }
    }

    /**
     * Delete a file without throwing any exception
     *
     * @param path
     * @return
     */
    private boolean deleteFileNoThrow( String path ) {
        File file;
        try {
            file = new File( path );
        } catch ( NullPointerException e ) {
            return false;
        }

        if ( file.exists() ) {
            return file.delete();
        }
        return false;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        mGalleryButton = (Button) findViewById( R.id.button1 );
        mEditButton = (Button) findViewById( R.id.button2 );
        mImage = ( (ImageView) findViewById( R.id.image ) );
        mImageContainer = findViewById( R.id.image_container );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {

        Intent intent;

        final int id = item.getItemId();

        if ( id == R.id.view_documentation ) {
            intent = new Intent( Intent.ACTION_VIEW );
            intent.setData( Uri.parse( "http://www.aviary.com/android-documentation" ) );
            startActivity( intent );
        } else if ( id == R.id.get_sdk ) {

            intent = new Intent( Intent.ACTION_VIEW );
            intent.setData( Uri.parse( "https://github.com/AviaryInc/Mobile-Feather-SDK-for-Android" ) );
            startActivity( intent );
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    protected Dialog onCreateDialog( int id ) {
        Dialog dialog = null;
        switch ( id ) {
//            // external sdcard is not mounted!
//            case EXTERNAL_STORAGE_UNAVAILABLE:
//                dialog = new AlertDialog.Builder( this ).setTitle( R.string.external_storage_na_title )
//                        .setMessage( R.string.external_storage_na_message ).create();
//                break;
        }
        return dialog;
    }

    @Override
    /**
     * This method is called when feather has completed ( ie. user clicked on "done" or just exit the activity without saving ). <br />
     * If user clicked the "done" button you'll receive RESULT_OK as resultCode, RESULT_CANCELED otherwise.
     *
     * @param requestCode
     * 	- it is the code passed with startActivityForResult
     * @param resultCode
     * 	- result code of the activity launched ( it can be RESULT_OK or RESULT_CANCELED )
     * @param data
     * 	- the result data
     */
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if ( resultCode == RESULT_OK ) {
            switch ( requestCode ) {
                case ACTION_REQUEST_GALLERY:
                    // user chose an image from the gallery
                    loadAsync( data.getData() );
                    break;

                case ACTION_REQUEST_FEATHER:

                    boolean changed = true;

                    if( null != data ) {
                        Bundle extra = data.getExtras();
                        if( null != extra ) {
                            // image was changed by the user?
                            changed = extra.getBoolean( Constants.EXTRA_OUT_BITMAP_CHANGED );
                        }
                    }

                    if( !changed ) {
                        Log.w( LOG_TAG, "User did not modify the image, but just clicked on 'Done' button" );
                    }

                    // send a notification to the media scanner
                    updateMedia( mOutputFilePath );

                    // update the preview with the result
                    loadAsync( data.getData() );
                    mOutputFilePath = null;
                    break;
            }
        } else if ( resultCode == RESULT_CANCELED ) {
            switch ( requestCode ) {
                case ACTION_REQUEST_FEATHER:

                    // delete the result file, if exists
                    if ( mOutputFilePath != null ) {
                        deleteFileNoThrow( mOutputFilePath );
                        mOutputFilePath = null;
                    }
                    break;
            }
        }
    }

    /**
     * Given an Uri load the bitmap into the current ImageView and resize it to fit the image container size
     *
     * @param uri
     */
    @SuppressWarnings("deprecation")
    private boolean setImageURI( final Uri uri, final Bitmap bitmap ) {

        Log.d( LOG_TAG, "image size: " + bitmap.getWidth() + "x" + bitmap.getHeight() );
        mImage.setImageBitmap( bitmap );
        mImage.setBackgroundDrawable( null );

        mEditButton.setEnabled( true );
        mImageUri = uri;

        return true;
    }

    /**
     * We need to notify the MediaScanner when a new file is created.
     * In this way all the gallery applications will be notified too.
     *
     * @param filepath
     */
    private void updateMedia( String filepath ) {
        Log.i( LOG_TAG, "updateMedia: " + filepath );
        MediaScannerConnection.scanFile( getApplicationContext(), new String[] { filepath }, null, null );
    }

    /**
     * Pick a random image from the user gallery
     *
     * @return
     */
    @SuppressWarnings("unused")
    private Uri pickRandomImage() {
        Cursor c = getContentResolver().query( Images.Media.EXTERNAL_CONTENT_URI, new String[] { ImageColumns._ID, ImageColumns.DATA },
                ImageColumns.SIZE + ">?", new String[] { "90000" }, null );
        Uri uri = null;

        if ( c != null ) {
            int total = c.getCount();
            int position = (int) ( Math.random() * total );
            Log.d( LOG_TAG, "pickRandomImage. total images: " + total + ", position: " + position );
            if ( total > 0 ) {
                if ( c.moveToPosition( position ) ) {
                    String data = c.getString( c.getColumnIndex( ImageColumns.DATA ) );
                    long id = c.getLong( c.getColumnIndex( ImageColumns._ID ) );

                    // you can pass to the Aviary-SDK an uri with a "content://" scheme
                    // or an absolute file path like "file:///mnt/..." or just "/mnt/..."

                    // using the "content:/" style uri
                    // uri = Uri.withAppendedPath( Images.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );

                    // using the file scheme uri, passing the real path
                    uri = Uri.parse( data );

                    Log.d( LOG_TAG, uri.toString() );
                }
            }
            c.close();
        }
        return uri;
    }

    /**
     * Return the current application version string
     *
     * @return
     */
    private String getLibraryVersion() {
        String result = "";

        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo( getPackageName(), 0 );
            result = info.versionName;
        } catch ( Exception e ) {}

        return result;
    }

    /**
     * Return a new image file. Name is based on the current time. Parent folder will be the one created with createFolders
     *
     * @return
     * @see #createFolders()
     */
    private File getNextFileName() {
        if ( mGalleryFolder != null ) {
            if ( mGalleryFolder.exists() ) {
                File file = new File( mGalleryFolder, "aviary_" + System.currentTimeMillis() + ".jpg" );
                return file;
            }
        }
        return null;
    }

    /**
     * Once you've chosen an image you can start the feather activity
     *
     * @param uri
     */
    @SuppressWarnings("deprecation")
    private void startFeather( Uri uri ) {

        Log.d( LOG_TAG, "uri: " + uri );

        // first check the external storage availability
        if ( !isExternalStorageAvailable() ) {
            showDialog( EXTERNAL_STORAGE_UNAVAILABLE );
            return;
        }

        // create a temporary file where to store the resulting image
        File file = getNextFileName();


        if ( null != file ) {
            mOutputFilePath = file.getAbsolutePath();
        } else {
            new AlertDialog.Builder( this ).setTitle( android.R.string.dialog_alert_title ).setMessage( "Failed to create a new File" )
                    .show();
            return;
        }

        // Create the intent needed to start feather
        Intent newIntent = new Intent( this, FeatherActivity.class );

        // === INPUT IMAGE URI (MANDATORY) ===
        // Set the source image uri
        newIntent.setData( uri );

        // === API KEY SECRET (MANDATORY) ====
        // You must pass your Aviary key secret
        newIntent.putExtra( Constants.EXTRA_IN_API_KEY_SECRET, API_SECRET );

        // <pcn:true>
        // == BILLING public key (MANDATORY if you're a PCN partner) ===
        // You can find your app's base64-encoded
        // public key in your application's page on Google Play Developer Console.
        // Note that this is NOT your "developer public key".
        // For more info: http://developer.android.com/training/in-app-billing/preparing-iab-app.html
        newIntent.putExtra( Constants.EXTRA_IN_BILLING_PUBLIC_KEY, BILLING_API );
        // </pcn:true>


        // === OUTPUT (OPTIONAL/RECOMMENDED)====
        // Pass the uri of the destination image file.
        // This will be the same uri you will receive in the onActivityResult
        newIntent.putExtra( Constants.EXTRA_OUTPUT, Uri.parse( "file://" + mOutputFilePath ) );

        // === OUTPUT FORMAT (OPTIONAL) ===
        // Format of the destination image
        newIntent.putExtra( Constants.EXTRA_OUTPUT_FORMAT, Bitmap.CompressFormat.JPEG.name() );

        // === OUTPUT QUALITY (OPTIONAL) ===
        // Output format quality (jpeg only)
        newIntent.putExtra( Constants.EXTRA_OUTPUT_QUALITY, 90 );


        // === WHITE LABEL (OPTIONAL/PREMIUM ONLY) ===
        // If you want to hide the 'feedback' button and the 'aviary' logo
        // pass this intent-extra
        // Note that you need to have the 'whitelabel' permissions enabled in order
        // to use this extra
        newIntent.putExtra( Constants.EXTRA_WHITELABEL, true );

        // === HIRES (OPTIONAL/PREMIUM ONLY) ===
        // If you want hires image processing pass this intent-extra with the maximum size
        // megapixels you want the saved image to be
        // Note that you need to have the 'hires' permissions enabled in order to use this extra
        newIntent.putExtra( Constants.EXTRA_IN_HIRES_MEGAPIXELS, MegaPixels.Mp5.ordinal() );

        // == TOOLS LIST ===
        // Optional
        // You can force feather to display only some tools ( see FilterLoaderFactory#Filters )
        // you can omit this if you just want to display the default tools


//		 newIntent.putExtra( "tools-list", new String[] {
//			 FilterLoaderFactory.Filters.ENHANCE.name(),
//			 FilterLoaderFactory.Filters.EFFECTS.name(),
//			 FilterLoaderFactory.Filters.STICKERS.name(),
//			 FilterLoaderFactory.Filters.CROP.name(),
//			 FilterLoaderFactory.Filters.BLUR.name(),
//			 FilterLoaderFactory.Filters.BRIGHTNESS.name(),
//			 FilterLoaderFactory.Filters.CONTRAST.name(),
//			 FilterLoaderFactory.Filters.SATURATION.name(),
//			 FilterLoaderFactory.Filters.SHARPNESS.name(),
//			 FilterLoaderFactory.Filters.SPLASH.name(),
//			 FilterLoaderFactory.Filters.DRAW.name(),
//			 FilterLoaderFactory.Filters.TEXT.name(),
//			 FilterLoaderFactory.Filters.REDEYE.name(),
//			 FilterLoaderFactory.Filters.WHITEN.name(),
//			 FilterLoaderFactory.Filters.BLEMISH.name(),
//			 FilterLoaderFactory.Filters.MEME.name(),
//		 } );


        // === EXIT ALERT (OPTIONAL) ===
        // You want to hide the exit alert dialog shown when back is pressed
        // without saving image first
        // newIntent.putExtra( Constants.EXTRA_HIDE_EXIT_UNSAVE_CONFIRMATION, true );

        // === VIBRATION (OPTIONAL) ===
        // Some aviary tools use the device vibration in order to give a better experience
        // to the final user. But if you want to disable this feature, just pass
        // any value with the key "tools-vibration-disabled" in the calling intent.
        // This option has been added to version 2.1.5 of the Aviary SDK
        // newIntent.putExtra( Constants.EXTRA_TOOLS_DISABLE_VIBRATION, true );

        // === NO CHANGES (OPTIONAL) ==
        // With this extra param you can tell to FeatherActivity how to manage
        // the press on the Done button even when no real changes were made to
        // the image.
        // If the value is true then the image will be always saved, a RESULT_OK
        // will be returned to your onActivityResult and the result Bundle will
        // contain an extra value "EXTRA_OUT_BITMAP_CHANGED" indicating if the
        // image was changed during the session.
        // If "false" is passed then a RESULT_CANCEL will be sent when an user will
        // hit the 'Done' button without any modifications ( also the EXTRA_OUT_BITMAP_CHANGED
        // extra will be sent back to the onActivityResult.
        // By default this value is true ( even if you omit it )
        newIntent.putExtra( Constants.EXTRA_IN_SAVE_ON_NO_CHANGES, true );

        // ..and start feather
        startActivityForResult( newIntent, ACTION_REQUEST_FEATHER );
    }

    /**
     * Check the external storage status
     *
     * @return
     */
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /**
     * Start the activity to pick an image from the user gallery
     */
    private void pickFromGallery() {
        Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
        intent.setType( "image/*" );

        Intent chooser = Intent.createChooser( intent, "Choose a Picture" );
        startActivityForResult( chooser, ACTION_REQUEST_GALLERY );
    }

    /**
     * Try to create the required folder on the sdcard where images will be saved to.
     *
     * @return
     */
    private File createFolders() {

        File baseDir;

        if ( android.os.Build.VERSION.SDK_INT < 8 ) {
            baseDir = Environment.getExternalStorageDirectory();
        } else {
            baseDir = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES );
        }

        if ( baseDir == null ) return Environment.getExternalStorageDirectory();

        Log.d( LOG_TAG, "Pictures folder: " + baseDir.getAbsolutePath() );
        File aviaryFolder = new File( baseDir, FOLDER_NAME );

        if ( aviaryFolder.exists() ) return aviaryFolder;
        if ( aviaryFolder.mkdirs() ) return aviaryFolder;

        return Environment.getExternalStorageDirectory();
    }

    class DownloadAsync extends AsyncTask<Uri, Void, Bitmap> implements OnCancelListener {

        ProgressDialog mProgress;
        private Uri mUri;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgress = new ProgressDialog( MainActivity.this );
            mProgress.setIndeterminate( true );
            mProgress.setCancelable( true );
            mProgress.setMessage( "Loading image..." );
            mProgress.setOnCancelListener( this );
            mProgress.show();
        }

        @Override
        protected Bitmap doInBackground( Uri... params ) {
            mUri = params[0];

            Bitmap bitmap = null;

            while ( mImageContainer.getWidth() < 1 ) {
                try {
                    Thread.sleep( 1 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }

            final int w = mImageContainer.getWidth();
            Log.d( LOG_TAG, "width: " + w );
            com.aviary.android.feather.library.utils.ImageInfo info = new com.aviary.android.feather.library.utils.ImageInfo();
            bitmap = DecodeUtils.decode( MainActivity.this, mUri, imageWidth, imageHeight, info );
            return bitmap;
        }

        @Override
        protected void onPostExecute( Bitmap result ) {
            super.onPostExecute( result );

            if ( mProgress.getWindow() != null ) {
                mProgress.dismiss();
            }

            if ( result != null ) {
                setImageURI( mUri, result );
            } else {
                Toast.makeText( MainActivity.this, "Failed to load image " + mUri, Toast.LENGTH_SHORT ).show();
            }
        }

        @Override
        public void onCancel( DialogInterface dialog ) {
            Log.i( LOG_TAG, "onProgressCancel" );
            this.cancel( true );
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.i( LOG_TAG, "onCancelled" );
        }

    }

    class ApiKeyReader extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground( Void... params ) {
            return SDKUtils.getApiKey( getBaseContext() );
        }

        @Override
        protected void onPostExecute( String result ) {
            super.onPostExecute( result );
            setApiKey( result );
        }

    }


}
