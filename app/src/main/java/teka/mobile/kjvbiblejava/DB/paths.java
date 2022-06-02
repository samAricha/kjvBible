package teka.mobile.kjvbiblejava.DB;

import java.io.File;

public class paths {

    public static final String bible_path =  "/data/data/com.example.kjvbiblejava/databases/";
    //public static final String songs_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCEA/songs";

    public static final int VERSE_ONLY = 0, VERSE_IMAGE_MESSAGE = 1, MESSAGE = 2, MESSAGE_IMAGE = 3, VERSE_MESSAGE = 4, ANNOUNCEMENT_IMAGE = 5, ANNOUNCEMENT = 6, BULLETIN = 7, IMAGE_ONLY = 8;


    public static boolean bibleAvailable() {
        File file = new File(bible_path+"bible.db");
        if (!file.exists()) {
            return false;
        }
        if (file.length() > 5700000) {
            return true;
        }
        return false;
    }
}