package com.example.papersoft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PaperSoftPrefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    Button selectImagesButton;
    Button generatePdfButton;
    TextView imageCountText;
    EditText fileNameEditText;

    Button openPdfButton;
    Button sharePdfButton;

    ImageButton themeToggleButton;

    List<Uri> selectedImages;

    ActivityResultLauncher<String> imagePicker;

    LinearLayout imageListContainer;

    File generatedPdfFile;

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentMode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int newMode;
        String toastText;

        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
            toastText = "Light mode enabled";
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            toastText = "System default theme enabled";
        } else {
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
            toastText = "Dark mode enabled";
        }

        prefs.edit().putInt(KEY_NIGHT_MODE, newMode).apply();
        AppCompatDelegate.setDefaultNightMode(newMode);
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    private void openPdf() {

        if (generatedPdfFile == null || !generatedPdfFile.exists()) {
            Toast.makeText(this, "PDF not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri pdfUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                generatedPdfFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf() {

        if (generatedPdfFile == null || !generatedPdfFile.exists()) {
            Toast.makeText(this, "PDF not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri pdfUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                generatedPdfFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share PDF"));
    }

    private void generatePdf() {

        if (selectedImages == null || selectedImages.isEmpty()) {
            imageCountText.setText("Please select images first");
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();

        try {
            for (int i = 0; i < selectedImages.size(); i++) {

                Uri imageUri = selectedImages.get(i);

                InputStream inputStream = getContentResolver().openInputStream(imageUri);

                if (inputStream == null) {
                    continue;
                }

                Bitmap bitmap;
                try {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } finally {
                    inputStream.close();
                }

                if (bitmap == null) {
                    continue;
                }

                PdfDocument.PageInfo pageInfo =
                        new PdfDocument.PageInfo.Builder(595, 842, i + 1).create();

                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                Canvas canvas = page.getCanvas();

                int pageWidth = 595;
                int pageHeight = 842;

                int imageWidth = bitmap.getWidth();
                int imageHeight = bitmap.getHeight();

                float scale = Math.min(
                        (float) pageWidth / imageWidth  ,
                        (float) pageHeight / imageHeight
                );

                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;

                float left = (pageWidth - scaledWidth) / 2f;
                float top = (pageHeight - scaledHeight) / 2f;

                RectF destination = new RectF(
                        left,
                        top,
                        left + scaledWidth,
                        top + scaledHeight
                );

                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                paint.setDither(true);

                canvas.drawBitmap(bitmap, null, destination, paint);

                pdfDocument.finishPage(page);

                bitmap.recycle();
            }

            // Determine custom filename from user input or fallback
            String inputName = fileNameEditText.getText().toString().trim();
            if (inputName.isEmpty()) {
                inputName = "PaperSoft_Doc";
            }

            // Sanitize filename for invalid filesystem characters
            inputName = inputName.replaceAll("[\\\\/:*?\"<>|]", "_");

            if (!inputName.toLowerCase().endsWith(".pdf")) {
                inputName = inputName + ".pdf";
            }

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                // Folder exists or was created
            }

            File pdfFile = new File(downloadsDir, inputName);
            int count = 1;
            String baseName = inputName.endsWith(".pdf") ? inputName.substring(0, inputName.length() - 4) : inputName;
            while (pdfFile.exists()) {
                pdfFile = new File(downloadsDir, baseName + "_" + count + ".pdf");
                count++;
            }

            generatedPdfFile = pdfFile;

            try (FileOutputStream outputStream = new FileOutputStream(generatedPdfFile)) {
                pdfDocument.writeTo(outputStream);
            }

            // Index file so it immediately appears in Downloads & PDF viewer apps
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{generatedPdfFile.getAbsolutePath()},
                    new String[]{"application/pdf"},
                    null
            );

            openPdfButton.setEnabled(true);
            sharePdfButton.setEnabled(true);

            imageCountText.setText("Saved to Downloads: " + generatedPdfFile.getName());
            Toast.makeText(this, "Saved to Downloads/" + generatedPdfFile.getName(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            imageCountText.setText("Error creating PDF");
        } finally {
            pdfDocument.close();
        }
    }

    private void refreshImageList() {

        imageListContainer.removeAllViews();

        if (selectedImages == null) {
            return;
        }

        for (int i = 0; i < selectedImages.size(); i++) {

            final int position = i;

            LinearLayout row = new LinearLayout(this);
            row.setBackgroundResource(R.drawable.bg_card_shape);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, 16);
            row.setLayoutParams(rowParams);

            row.setPadding(16, 16, 16, 16);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView imageView = new ImageView(this);
            imageView.setImageURI(selectedImages.get(i));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(120, 120);

            row.addView(imageView, imageParams);

            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.VERTICAL);
            controls.setPadding(16, 0, 0, 0);

            Button upButton = new Button(this);
            upButton.setText("Up");
            upButton.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            Button downButton = new Button(this);
            downButton.setText("Down");
            downButton.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            Button deleteButton = new Button(this);
            deleteButton.setText("Delete");
            deleteButton.setTextColor(ContextCompat.getColor(this, R.color.outline_text));

            upButton.setEnabled(position > 0);
            downButton.setEnabled(position < selectedImages.size() - 1);

            upButton.setOnClickListener(v -> {
                Uri image = selectedImages.remove(position);
                selectedImages.add(position - 1, image);
                refreshImageList();
            });

            downButton.setOnClickListener(v -> {
                Uri image = selectedImages.remove(position);
                selectedImages.add(position + 1, image);
                refreshImageList();
            });

            deleteButton.setOnClickListener(v -> {
                selectedImages.remove(position);
                imageCountText.setText(selectedImages.size() + " images selected");
                refreshImageList();
            });

            controls.addView(upButton);
            controls.addView(downButton);
            controls.addView(deleteButton);

            row.addView(controls);
            imageListContainer.addView(row);
        }

        imageCountText.setText(selectedImages.size() + " images selected");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImagesButton = findViewById(R.id.selectImagesButton);
        generatePdfButton = findViewById(R.id.generatePdfButton);
        imageCountText = findViewById(R.id.imageCountText);
        fileNameEditText = findViewById(R.id.fileNameEditText);
        imageListContainer = findViewById(R.id.imageListContainer);
        openPdfButton = findViewById(R.id.openPdfButton);
        sharePdfButton = findViewById(R.id.sharePdfButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    selectedImages = new ArrayList<>(uris);
                    imageCountText.setText(selectedImages.size() + " images selected");
                    refreshImageList();
                }
        );

        selectImagesButton.setOnClickListener(v -> imagePicker.launch("image/*"));
        generatePdfButton.setOnClickListener(v -> generatePdf());
        openPdfButton.setOnClickListener(v -> openPdf());
        sharePdfButton.setOnClickListener(v -> sharePdf());
        themeToggleButton.setOnClickListener(v -> toggleTheme());
    }
}
