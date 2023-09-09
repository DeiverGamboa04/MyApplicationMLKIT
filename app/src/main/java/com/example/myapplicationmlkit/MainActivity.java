package com.example.myapplicationmlkit;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.RGBLuminanceSource;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;

    private Button btCamera;
    private Button btGallery;
    private EditText etQRCode;
    private ImageView ivQRCode;
    private boolean isScanningFromCamera = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btCamera = findViewById(R.id.btCamera);
        btGallery = findViewById(R.id.btGallery);
        etQRCode = findViewById(R.id.et_qr_code);
        ivQRCode = findViewById(R.id.iv_qr_code);

        btCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    isScanningFromCamera = true;
                    ivQRCode.setVisibility(View.GONE);
                    startCameraScanner();
                }
            }
        });
        btGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScanningFromCamera = false;
                ivQRCode.setVisibility(View.VISIBLE);
                // Abrir la galería para seleccionar una imagen
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_REQUEST);
            }
        });
    }

    private void startCameraScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isScanningFromCamera) {
                    startCameraScanner();
                }
            } else {
                Toast.makeText(this, "No se han concedido los permisos para la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            // Resultado de la imagen de la galería
            Uri uri = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bitmap != null) {
                // Escanear el código QR desde la imagen utilizando ZXing
                String qrCodeText = scanQRCode(bitmap);
                if (qrCodeText != null) {
                    etQRCode.setText(qrCodeText);
                } else {
                    etQRCode.setText("No se pudo detectar un código QR");
                }
            }
        } else if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            // Resultado de la cámara en tiempo real
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                String qrCodeText = result.getContents();
                if (qrCodeText != null) {
                    etQRCode.setText(qrCodeText);
                } else {
                    etQRCode.setText("No se pudo detectar un código QR");
                }
            }
        }
    }
    @Nullable
    private String scanQRCode(Bitmap bitmap) {
        try {
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            // Convierte el bitmap en un arreglo de enteros
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            HybridBinarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);

            Result result = new QRCodeReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            Log.e(TAG, "Error al escanear el código QR", e);
            return null;
        }
    }

}