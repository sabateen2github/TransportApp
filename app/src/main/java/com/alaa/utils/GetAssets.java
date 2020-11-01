package com.alaa.utils;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GetAssets {

    private static boolean checked = false;

    private static void updateAssetFile(Application application, boolean exist) throws IOException {
        File file = new File(application.getCacheDir(), "assets.zip");
        File idFile = new File(application.getCacheDir(), "assetsId.id");

        if (file.exists())
            file.delete();
        if (idFile.exists())
            idFile.delete();

        file.createNewFile();
        idFile.createNewFile();

        try {
            URL url = new URL("http://alaa4sabateen.000webhostapp.com/assets.zip");
            FileOutputStream writer = new FileOutputStream(file);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            byte[] data = new byte[4096];
            int read;
            InputStream inputStream = connection.getInputStream();
            while ((read = inputStream.read(data)) > 0) {
                writer.write(data, 0, read);
            }
            connection.disconnect();
            writer.close();
        } catch (MalformedURLException e) {
            Log.e("Alaa", "Error 5 : ");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("Alaa", "Error 6 : ");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Alaa", "Error 7 : " + e);
        }


        try {
            URL url = new URL("http://alaa4sabateen.000webhostapp.com/assetsId.id");
            FileOutputStream writer = new FileOutputStream(idFile);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            byte[] data = new byte[4096];
            int read;
            InputStream inputStream = connection.getInputStream();
            while ((read = inputStream.read(data)) > 0) {
                writer.write(data, 0, read);
            }
            connection.disconnect();
            writer.close();
        } catch (MalformedURLException e) {
            Log.e("Alaa", "Error 8 : ");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            Log.e("Alaa", "Error 9 : ");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Alaa", "Error 10 : ");
            e.printStackTrace();
        }

    }

    public static InputStream open(Application application, String FileName) throws FileNotFoundException {


        Log.e("Alaa", "open : " + FileName);
        File file = new File(application.getCacheDir(), "assets.zip");
        File idFile = new File(application.getCacheDir(), "assetsId.id");


        if (!checked && idFile.exists()) {
            try {
                URL check = new URL("http://alaa4sabateen.000webhostapp.com/assetsId.id");
                HttpURLConnection connection = (HttpURLConnection) check.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String newID = reader.readLine();
                BufferedReader fileReader = new BufferedReader(new FileReader(idFile));
                String oldID = fileReader.readLine();
                connection.disconnect();
                fileReader.close();
                if (!oldID.equals(newID)) {
                    Log.e("Alaa", "Not same ID");
                    updateAssetFile(application, true);
                }
            } catch (MalformedURLException e) {
                Log.e("Alaa", "Error 1 : ");

                e.printStackTrace();
            } catch (IOException e) {
                Log.e("Alaa", "Error 2 : ");

                e.printStackTrace();
            }
        } else if (!idFile.exists() || !file.exists()) {
            try {
                updateAssetFile(application, false);
            } catch (IOException e) {
                Log.e("Alaa", "Error 3 : ");

                e.printStackTrace();
            }
        }

        checked = true;

        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry("assets/" + FileName);
            return zipFile.getInputStream(entry);

        } catch (IOException e) {
            Log.e("Alaa", "Error 4 : ");

            e.printStackTrace();
        }

        throw new FileNotFoundException(FileName);
    }

}
