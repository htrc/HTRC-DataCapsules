/*******************************************************************************
 * Copyright 2019 The Trustees of Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package edu.indiana.d2i.sloan.utils;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.exception.NoResultFileFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class ResultUtils {
    private static Logger logger = Logger.getLogger(ResultUtils.class);
    private static String RESULT_DIR_LOCATION = Configuration.getInstance().getString(
            Configuration.PropertyName.RESULT_FILES_DIR, "/tmp") + "/%s";
    private static String RESULT_FILE_LOCATION = Configuration.getInstance().getString(
            Configuration.PropertyName.RESULT_FILES_DIR, "/tmp") + "/%s/results.zip";

    public static void saveResultFile(String result_id, InputStream inputStream) throws IOException {
        Path dirPath = Paths.get(String.format(RESULT_DIR_LOCATION, result_id));
        Path filePath = Paths.get(String.format(RESULT_FILE_LOCATION, result_id));
        if(!Files.exists(dirPath)) Files.createDirectory(dirPath);
        if(!Files.exists(filePath)) Files.createFile(filePath);
        Files.copy(inputStream,filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void saveResultFileToDir(String result_id, InputStream inputStream, String dir) throws IOException {
        Path dirPath = Paths.get(dir + "/" + result_id);
        Path filePath = Paths.get(dir + "/" + result_id + "/results.zip");
        if(!Files.exists(dirPath)) Files.createDirectory(dirPath);
        if(!Files.exists(filePath)) Files.createFile(filePath);
        Files.copy(inputStream,filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static InputStream getResultFile(String result_id) throws IOException, NoResultFileFoundException {
        Path filePath = Paths.get(String.format(RESULT_FILE_LOCATION, result_id));
        if(!Files.exists(filePath)) {
            logger.error("No result file found for result ID " + result_id);
            throw new NoResultFileFoundException("No result file found for result ID " + result_id);
        }
        return Files.newInputStream(filePath);
    }
}
