/*
 * Copyright 2013 Juan Sepulveda 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
	
	Under LGP3-license and the Apache 2.0 license part of this project was derived from ( available at http://opensource.org/licenses/lgpl-3.0.html and http://www.apache.org/licenses/LICENSE-2.0)
	
	github tesseract tess-two: available at https://github.com/rmtheis/tess-two
	
	Javaluator: available at http://sourceforge.net/projects/javaluator/files/latest/download?utm_expid=65835818-0&utm_referrer=http%3A%2F%2Fjavaluator.sourceforge.net%2Fen%2Fhome%2F
	(home at http://javaluator.sourceforge.net/en/home/)
  
  */
package cam.calc;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cam.calc.R;
import com.googlecode.tesseract.android.TessBaseAPI;


public class Main extends Activity {
	public static final String PACKAGE_NAME = "com.cam.calc";
	public static final String DATA_PATH = Environment
			.getExternalStorageDirectory().toString() + "/SimpleAndroidOCR/";
	
	// You should have the trained data file in assets folder
	// You can get them at:
	// http://code.google.com/p/tesseract-ocr/downloads/list
	public static final String lang = "eng";

	private static final String TAG = "Main.java";

	protected Button _button;
	// protected ImageView _image;
	//protected TextView _field;
	protected TextView _field2;
	protected TextView _field3;
	protected String _path;
	protected ImageView _image;
	protected boolean _taken;

	protected static final String PHOTO_TAKEN = "photo_taken";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}

		}
		
		// lang.traineddata file with the app (in assets folder)
		// You can get them at:
		// http://code.google.com/p/tesseract-ocr/downloads/list
		// This area needs work and optimization
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/eng.traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH
						+ "tessdata/eng.traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				//gin.close();
				out.close();
				
				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			} 
		}

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		_image = (ImageView) findViewById(R.id.imageView1);
		//_field = (TextView) findViewById(R.id.textView1);
		_field3 = (TextView) findViewById(R.id.textView3);
		_field2 = (TextView) findViewById(R.id.textView2);
		_button = (Button) findViewById(R.id.button1);
		_button.setOnClickListener(new ButtonClickHandler());

		_path = DATA_PATH + "/ocr.jpg";
	}

	public class ButtonClickHandler implements View.OnClickListener {
		public void onClick(View view) {
			Log.v(TAG, "Starting Camera app");
			startCameraActivity();
		}
	}

	// Simple android photo capture:
	// http://labs.makemachine.net/2010/03/simple-android-photo-capture/

	protected void startCameraActivity() {
		File file = new File(_path);
		Uri outputFileUri = Uri.fromFile(file);

		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "resultCode: " + resultCode);

		if (resultCode == -1) {
			onPhotoTaken();
		} else {
			Log.v(TAG, "User cancelled");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(Main.PHOTO_TAKEN, _taken);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState()");
		if (savedInstanceState.getBoolean(Main.PHOTO_TAKEN)) {
			onPhotoTaken();
		}
	}

	protected void onPhotoTaken() {
		_taken = true;
		_field2.setVisibility(View.GONE);  
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;

		Bitmap bitmap = BitmapFactory.decodeFile(_path, options);

		try {
			ExifInterface exif = new ExifInterface(_path);
			int exifOrientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			Log.v(TAG, "Orient: " + exifOrientation);

			int rotate = 0;

			switch (exifOrientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			}

			Log.v(TAG, "Rotation: " + rotate);

			if (rotate != 0) {

				// Getting width & height of the given image.
				int w = bitmap.getWidth();
				int h = bitmap.getHeight();

				// Setting pre rotate
				Matrix mtx = new Matrix();
				mtx.preRotate(rotate);

				// Rotating Bitmap
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			}

			// Convert to ARGB_8888, required by tess
			bitmap = toGrayscale(bitmap);
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		} catch (IOException e) {
			Log.e(TAG, "Couldn't correct orientation: " + e.toString());
		}

		
		
		Log.v(TAG, "Before baseApi");

		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.setDebug(true);
		baseApi.init(DATA_PATH, lang);
		baseApi.setImage(bitmap);
		
		String recognizedText = baseApi.getUTF8Text();
		
		baseApi.end();

		// You now have the text in recognizedText var, you can do anything with it.
		// We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)

		Log.v(TAG, "OCRED TEXT: " + recognizedText);
		

		 if ( lang.equalsIgnoreCase("eng") ) {
			recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
		}
		
		recognizedText = recognizedText.trim();
		
		if ( recognizedText.length() != 0 ) {
			//_field.setText(recognizedText); 
			_field3.setText(recognizedText); 
		}
		
		_image.setImageBitmap( bitmap );
		// Cycle done.
	} public static Bitmap toGrayscale(Bitmap bmpOriginal){        
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();    

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/***** EXPERIMENTAL: Duplicate + MATH PROCESSOR MEANT TO USE TESS OUTPUT AS INPUT ****/
	
	
	/*
	ImageView iv;
	TextView tv;
	protected Bitmap _image; 
	protected Bitmap bmp; 
	public static String The_path = "/mnt/sdcard/DCIM/100MEDIA/TESS.jpg";
	public static String lang = "eng";
	public static String lang2 = "equ";
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/SimpleAndroidOCR/";
	private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	try {

    		AssetManager assetManager = getAssets();
    		InputStream in = assetManager.open("tessdata/eng.traineddata");
    		//GZIPInputStream gin = new GZIPInputStream(in);
    		OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/eng.traineddata");

    		//Transfer bytes from in to out
    		byte[] buf = new byte[1024];
    		int len;
    		//while ((len = gin.read(buf)) > 0) {
    		while ((len = in.read(buf)) > 0) {
    			out.write(buf, 0, len);
    		}
    		in.close();
    		//gin.close();
    		out.close();
    		
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};  
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        iv = (ImageView) findViewById(R.id.imageView1);
        tv = (TextView) findViewById(R.id.textView1);
        
        Button but = (Button) findViewById(R.id.button1);
        Button but2 = (Button) findViewById(R.id.button2);
        but.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		//File file = new File( _path );
        		//Uri outputFileUri = Uri.fromFile( file );
        		
        		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        		//intent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );
        		startActivityForResult(intent, 0);
        	}
        });
        
        but2.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		TessBaseAPI baseApi = new TessBaseAPI();
        		baseApi.setDebug(true);
        		baseApi.setPageSegMode(pageSegmentationMode);
        		baseApi.init(DATA_PATH, lang); 
        		baseApi.init(DATA_PATH, lang2); 
        		// DATA_PATH = Path to the storage
        		// lang for which the language data exists, usually "eng"
        		try {     
        			baseApi.setImage(ReadFile.readBitmap(bmp));
        			String recognizedText = baseApi.getUTF8Text();
        			txt(recognizedText);

        		} catch(RuntimeException e) {

        		}
        		baseApi.end();

        	}

			private void txt(String txt) {
				// TODO Auto-generated method stub
				tv.setText(txt);
			}
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    	BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.inSampleSize = 4;
		
		Bitmap bitmap = BitmapFactory.decodeFile(The_path, options);
		try {
			ExifInterface exif = new ExifInterface(The_path);
			int exifOrientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			int rotate = 0;

			switch (exifOrientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			}

			if (rotate != 0) {

				// Getting width & height of the given image.
				int w = bitmap.getWidth();
				int h = bitmap.getHeight();

				// Setting pre rotate
				Matrix mtx = new Matrix();
				mtx.preRotate(rotate);

				// Rotating Bitmap
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			}
		bitmap = toGrayscale(bitmap);
    	bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		} catch (IOException e) {
		
		}
		_image = (Bitmap) data.getExtras().get("data");
		iv.setImageBitmap(_image);
		bmp = bitmap;
    	/*
    	TessBaseAPI baseApi = new TessBaseAPI();
		// DATA_PATH = Path to the storage
		// lang for which the language data exists, usually "eng"
		baseApi.init(_path, "eng");  baseApi.setImage(_image);
		String recognizedText = baseApi.getUTF8Text();
		baseApi.end();
    	
    	// *** MATH OPERATOR*** //
    	DoubleEvaluator evaluator = new DoubleEvaluator();
    	Double result = evaluator.evaluate(recognizedText);
    	tv.setText(result.toString());
   		* /
    } 
    public static Bitmap toGrayscale(Bitmap bmpOriginal){        
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();    

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    } */
}
