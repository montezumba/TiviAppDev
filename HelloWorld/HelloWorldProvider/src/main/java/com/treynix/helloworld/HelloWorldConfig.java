package com.treynix.helloworld;

import android.util.SparseArray;

import com.montezumba.lib.types.Constants;
import com.montezumba.lib.types.Playlist;
import com.treynix.tiviapplive.provider.config.AddonConfig;

import java.util.HashMap;
import java.util.Map;

public class HelloWorldConfig extends AddonConfig {


    public static final String PARAMETER_PATTERN = "#!@@!#";

    public static final String DEMO_GUIDE_1 = "hello_world_guide_1.xml";
    public static final String XMLTV_TEMP_PATH = "temp_guide.xml";
    public static final int MAX_TVGUIDE_DAYS = 3;
    public static final int TVGUIDE_REFRESH_HOURS = 8;
    public static final String DEMO_GUIDE_TEMPLATE = "https://www.dropbox.com/s/9d2nbt95nbych6p/helloWorldTvGuideTemplate.txt?dl=1";


	public HelloWorldConfig() {
		super(	"Hello World Provider",
				5 * Constants.MINUTES,
				5 * Constants.MINUTES,
				10,
				HelloWorldConfig.class.getPackage().getName(),
                null
                );
	}

	public static final Map<String, SparseArray<String>> ENCODED_CHANNELS = new HashMap<String, SparseArray<String>>() {{
	    put("demo", new SparseArray<String>() {{
            put(0, "https://download.blender.org/durian/movies/Sintel.2010.4k.mkv");
            put(1, "http://distribution.bbb3d.renderfarming.net/video/mp4/bbb_sunflower_1080p_60fps_normal.mp4"); //BBB 1080
            put(2, "http://distribution.bbb3d.renderfarming.net/video/mp4/bbb_sunflower_2160p_60fps_normal.mp4"); //BBB 4K
            put(3, "http://distribution.bbb3d.renderfarming.net/video/mp4/bbb_sunflower_1080p_60fps_stereo_arcd.mp4"); //BBB 1080 3D
            put(4, "http://www.valkaama.com/download.php?target=media/movie/Valkaama_1080_p.mkv");
        }});
    }};

    public enum PlaylistSource {
        HELLO_WORLD("Hello World Channels", "https://www.dropbox.com/s/w63c2wfgpwyqujv/hello_world.m3u?dl=1");

        PlaylistSource(String name, String path) {
            mPlaylist = new Playlist(name);
            mPlaylist.pathToPlaylist = path;
        }

        private Playlist mPlaylist;

        public Playlist getPlaylist() {
            return mPlaylist;
        }

    }


}



	

