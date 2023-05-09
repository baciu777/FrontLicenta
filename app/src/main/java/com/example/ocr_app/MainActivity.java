package com.example.ocr_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageView;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    ImageButton capture_button, gallery_button, swap_view_button;
    Button copy_button;
    Bitmap bitmap;
    TextView textView;
    ImageView imageView;
    ScrollView scrollView;
    ProgressDialog progressDialog;
    private PackageInfo mPackageInfo;
    private static final int CAMERA_GALLERY_PERM_CODE = 102;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int CAPTURE_REQUEST_CODE = 300;
    private static final String ipAddress = "192.168.0.125";
    private static final int port = 8080;
    Uri photoURI;
    Bitmap photoInUse;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        capture_button = findViewById(R.id.btn_capture);
        copy_button = findViewById(R.id.btn_copy);
        textView = findViewById(R.id.text_view);
        gallery_button = findViewById(R.id.btn_gallery);
        swap_view_button = findViewById(R.id.btn_swap);
        imageView = findViewById(R.id.image_view);
        scrollView = findViewById(R.id.scroll_view);

        // Initially hide copy button
        copy_button.setVisibility(View.INVISIBLE);
        swap_view_button.setVisibility(View.INVISIBLE);
        textView.setTextColor(Color.WHITE);


        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.MILLISECONDS)
                .readTimeout(1000, TimeUnit.MILLISECONDS)
                .writeTimeout(1000, TimeUnit.MILLISECONDS)
                .build();
        Request request = new Request.Builder().url("http://192.168.0.125:8080/").build();
        //async
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        capture_button.setEnabled(false);
                        gallery_button.setEnabled(false);
                        Toast.makeText(MainActivity.this, "Network not found", Toast.LENGTH_LONG).show();

                    }
                });
            }


            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                String responseBodyString = responseBody.string();
                runOnUiThread(new Runnable() {//altfel pusca ca cica ruleaza pe thread secundar
                    @Override
                    public void run() {
                        textView.setText(responseBodyString);
                    }
                });
            }
        });


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

        ) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, CAMERA_GALLERY_PERM_CODE);

        }


        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Camera permission is required to capture a photo", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File dir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

                File output = new File(dir, "CameraContent2.jpeg");
                photoURI = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", output);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


                startActivityForResult(intent, CAPTURE_REQUEST_CODE);


            }
        });
        gallery_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Gallery permission is required to upload a photo", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent, GALLERY_REQUEST_CODE);
            }
        });
        copy_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String copied_text = textView.getText().toString();
                copyToClipBoard(copied_text);
            }
        });

        swap_view_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (textView.getVisibility() == View.VISIBLE) {
                    textView.setVisibility(View.INVISIBLE);
                    imageView.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.INVISIBLE);
                    // if the textView is visible, hide it and show the image
                    swap_view_button.setImageResource(R.drawable.hide_image);
                    swap_view_button.setBackgroundResource(R.drawable.round_img);

                } else {
                    scrollView.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.INVISIBLE);
                    // if the textView is not visible, show it and hide the image
                    swap_view_button.setImageResource(R.drawable.hide_text);
                    swap_view_button.setBackgroundResource(R.drawable.round_img);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    photoInUse = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                    getTextFromImage(photoInUse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            // Call CropImage activity with the selected image URI
            CropImage.activity(selectedImage).setGuidelines(CropImageView.Guidelines.ON)
                    .start(MainActivity.this);//asta va apela if ul de mai sus

        }
        if (requestCode == CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {//&& data != null) {
            //susta e ca noi salvam poza deci data e null ca nu trimitem nimic
            File dir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File pictureFile = new File(dir, "CameraContent2.jpeg");

            //Uri selectedImage = data.getData();
            try {

                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);

                FileOutputStream fos = new FileOutputStream(pictureFile);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                Uri selectedImage = getImageUri(getApplicationContext(), bitmap);
                CropImage.activity(selectedImage).setGuidelines(CropImageView.Guidelines.ON)
                        .start(MainActivity.this);//asta va apela if ul de sus

            } catch (IOException e) {

                e.printStackTrace();
            }

        }
    }

    private void getTextFromImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.png",
                        RequestBody.create(MediaType.parse("image/*"), byteArray))
                .build();


        Request request = new Request.Builder()

                .url("http://" + ipAddress + ":" + port + "/image") // Replace with your server URL
                .post(requestBody)///
                .build();
        startProgressBar();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                progressDialog.dismiss();
                String responseText = response.body().string();
                try {
                    JSONObject responseData = new JSONObject(responseText);
                    String predictionText = responseData.getString("prediction");
                    if (predictionText.equals("")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialog("no words found");
                            }
                        });
                    }

                    String[] lines = predictionText.split("\n");

                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        sb.append(line).append("\n");
                    }
                    String multiLineText = sb.toString();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.setVisibility(View.VISIBLE);
                            textView.setText(multiLineText);
                            textView.setTextColor(Color.WHITE);
                            textView.setVisibility(View.VISIBLE);
                            copy_button.setVisibility(View.VISIBLE);
                            imageView.setVisibility(View.INVISIBLE);
                            swap_view_button.setVisibility(View.VISIBLE);



                            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            imageView.setImageBitmap(photoInUse);

                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void copyToClipBoard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(MainActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(message);
        builder1.setCancelable(false);

        builder1.setPositiveButton(

                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });


        AlertDialog alert11 = builder1.create();
        alert11.show();
    }


    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageTitle = "DigitalHand_" + timeStamp;
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imageTitle, null);
        return Uri.parse(path);
    }


    private void startProgressBar() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }


}