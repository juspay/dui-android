/*
* Copyright (c) 2012-2017 "JUSPAY Technologies"
* JUSPAY Technologies Pvt. Ltd. [https://www.juspay.in]
*
* This file is part of JUSPAY Platform.
*
* JUSPAY Platform is free software: you can redistribute it and/or modify
* it for only educational purposes under the terms of the GNU Affero General
* Public License (GNU AGPL) as published by the Free Software Foundation,
* either version 3 of the License, or (at your option) any later version.
* For Enterprise/Commerical licenses, contact <info@juspay.in>.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  The end user will
* be liable for all damages without limitation, which is caused by the
* ABUSE of the LICENSED SOFTWARE and shall INDEMNIFY JUSPAY for such
* damages, claims, cost, including reasonable attorney fee claimed on Juspay.
* The end user has NO right to claim any indemnification based on its use
* of Licensed Software. See the GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/agpl.html>.
*/


package in.juspay.mystique;

import android.content.Context;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by parth on 17/9/16.
 */
public class FileUtil {

    private static final String LOG_TAG = FileUtil.class.getSimpleName();

    public static byte[] getFileFromInternalStorageOrAssets(Context context, String fileName) throws IOException {
        byte[] data = null;
        data = getFileFromInternalStorage(context, fileName);
        if (data == null) {
            data = getFileFromAssets(context, fileName);
        }
        return data;
    }

    public static byte[] getFileFromExternalStorage(String basePath, String fileName) throws IOException {
        File file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + basePath, fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos = readFromInputStream(bos, fileInputStream);
        return bos.toByteArray();
    }

    public static byte[] getFileFromInternalStorage(Context context, String fileName) throws IOException {
        File file = new File(context.getDir("juspay", Context.MODE_PRIVATE), fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos = readFromInputStream(bos, fileInputStream);
        return bos.toByteArray();
    }

    public static byte[] getFileFromAssets(Context context, String fileName) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName, Context.MODE_PRIVATE);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos = readFromInputStream(bos, inputStream);
        return bos.toByteArray();
    }

    private static ByteArrayOutputStream readFromInputStream(ByteArrayOutputStream bos, InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        bos.close();
        is.close();
        return bos;
    }

    public static String md5(final byte[] bytes) throws NoSuchAlgorithmException {
        final String MD5 = "MD5";
        MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
        digest.update(bytes);
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            String h = Integer.toHexString(0xFF & aMessageDigest);
            while (h.length() < 2)
                h = "0" + h;
            hexString.append(h);
        }
        return hexString.toString();
    }

    public static void saveFileToInternalStorage(Context context, String fileName, byte[] data) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(context.getDir("juspay", Context.MODE_PRIVATE), fileName));
            outputStream.write(data);
        } finally {
            outputStream.close();
        }
    }

}
