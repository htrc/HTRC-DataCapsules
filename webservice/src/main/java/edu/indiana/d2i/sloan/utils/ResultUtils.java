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
    private static String result_file_dir = Configuration.getInstance().getString(
            Configuration.PropertyName.RESULT_FILES_DIR, "/tmp");
    private static String backup_file_dir = Configuration.getInstance().getString(
            Configuration.PropertyName.RESULT_BACKUP_FILES_DIR, "/tmp/result-backup");
    private static String RESULT_DIR_LOCATION = result_file_dir + "/%s";
    private static String RESULT_FILE_LOCATION = result_file_dir + "/%s/results.zip";
    private static String BACKUP_DIR_LOCATION = backup_file_dir + "/%s";
    private static String BACKUP_FILE_LOCATION = backup_file_dir + "/%s/results.zip";

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

    public static void backupResultFile(String result_id) throws IOException {
        Path destDirPath = Paths.get(String.format(BACKUP_DIR_LOCATION, result_id));
        Path destFilePath = Paths.get(String.format(BACKUP_FILE_LOCATION, result_id));
        Path srcDirPath = Paths.get(String.format(RESULT_DIR_LOCATION, result_id));
        Path srcFilePath = Paths.get(String.format(RESULT_FILE_LOCATION, result_id));
        if(!Files.exists(destDirPath)) Files.createDirectory(destDirPath);
        Files.move(srcFilePath,destFilePath, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(srcDirPath);
        logger.info("Result file with ID " + result_id + " was backed up from " + result_file_dir + " to " + backup_file_dir + " directory!");
    }
}
