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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Created by parth on 17/9/16.
 */
public class RemoteAssetService {

    private static final String LOG_TAG = RemoteAssetService.class.getSimpleName();

    public static Boolean downloadAndSaveFile(Context context, String url) throws IOException, NoSuchAlgorithmException {
        byte[] newFile = null;
        int index = url.lastIndexOf("/");
        String fileName = url.substring(index + 1);
        newFile = downloadFileIfNotModified(context, url);
        if (newFile != null) {
            FileUtil.saveFileToInternalStorage(context, fileName, newFile);
        } else {
            return false;
        }
        return true;
    }

    private static byte[] downloadFileIfNotModified(Context context, String url) throws IOException, NoSuchAlgorithmException {
        int index = url.lastIndexOf("/");
        String fileName = url.substring(index + 1);
        byte[] currentConfig = null;
        String currentHash = null;
        currentConfig = FileUtil.getFileFromInternalStorageOrAssets(context, fileName);
        if (currentConfig != null) {
            currentHash = FileUtil.md5(currentConfig);
        }

        // Appending timeStamp so that ISP doesnt cache and appending If-None_match header
        HashMap queryParam = new HashMap<String, String>();
        queryParam.put("ts", String.valueOf(System.currentTimeMillis()));
        queryParam.put("If-None-Match", currentHash);

        return RestClient.fetchIfModified(url, queryParam);
    }
}
