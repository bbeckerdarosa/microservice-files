package com.barbbecker.jtcore.readfiles;

import com.barbbecker.jtcore.readfiles.dto.DataDto;
import com.barbbecker.jtcore.readfiles.file.ReaderFile;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;


public class Watcher {

    private final static String QUEUE_NAME = "fileReportQueue";
    private final static String PATH_IN = System.getProperty("user.home") + "/data/in/";
    private final static String EXTENTION = ".dat";

    public static void startWatch(Channel channel) throws Exception {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path pathDirectory = Paths.get(PATH_IN);
            pathDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            System.out.println("Waiting files...");
            while (true) {
                WatchKey watchKey = null;
                watchKey = watchService.poll(10, TimeUnit.MINUTES);
                if (watchKey != null) {
                    watchKey.pollEvents().stream().forEach(event -> {
                        String pathFile = ((WatchEvent<Path>) event).context().toString();
                        String contentData = ReaderFile.readStringFile(Paths.get(PATH_IN + pathFile));

                        if (pathFile.endsWith(EXTENTION)) {
                            DataDto data = new DataDto(pathFile, contentData);

                            Gson gson = new Gson();
                            String dataJsonToString = gson.toJson(data);

                            try {
                                channel.basicPublish("", QUEUE_NAME, null, dataJsonToString.getBytes());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println(" [x] Sent " + contentData);

                        } else {
                            System.out.println("The file extension is not " + EXTENTION);
                        }
                    });
                }
                watchKey.reset();
            }
        }

    }
}