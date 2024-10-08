package com.kyagamy.step.game.newplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.VideoView;

import com.kyagamy.step.R;
import com.kyagamy.step.common.Common;

import java.util.ArrayList;

public class BgPlayer {
    private VideoView player;
    private int currentBg = 0;
    private ArrayList<bgChange> BGList = new ArrayList<>();
    private Context context;
    boolean changeVideo = false, isRunning = false;
    private String path;
    private Double BPM;

    public BgPlayer(String path, String data, VideoView player, Context context, Double BPM) {
        assert data != null;
        assert player != null;
        assert context != null;
        assert path != null;
        this.context = context;
        this.path = path;
        this.player = player;
        this.BPM = BPM;
        String[] listBG = data.replace("\n", "").replace("\r", "").split(",");
        for (String bg : listBG) {
            BGList.add(new bgChange(bg));
        }
    }


    public void update(double beat) {
        if (isRunning && BGList.size() > currentBg) {
            bgChange bg = BGList.get(currentBg);
            if (changeVideo && bg.beat <= beat) {
                try {
                    SharedPreferences sharedPref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
                    String basePath = sharedPref.getString(context.getString(R.string.base_path), "Error path");

                    if (bg.fileName.contains(".")) {
                        String ext = bg.fileName.substring(bg.fileName.lastIndexOf("."));
                        ext = ext.toLowerCase();
                        switch (ext) {
                            case ".avi":
                            case ".mov":
                            case ".mp4":
                            case ".3gp":
                            case ".ogg":
                            case ".mpeg":
                            case ".mpg":
                            case ".flv":
                                String bgaDir = Common.Companion.checkBGADir(path, bg.fileName, basePath);
                                player.setBackground(null);
                                if (bgaDir != null) {
                                    player.setVideoPath(bgaDir);
                                    if (bg.beat < 0)
                                        player.seekTo((int) Math.abs(Common.Companion.beat2Second(bg.beat, BPM) * 1000 + 100));
                                    player.start();
                                } else
                                    playBgaOff();
                                break;
                            case ".png":
                            case ".webp":
                            case ".jpg":
                            case ".bpm":
                            case ".tiff":
                                bgaDir = Common.Companion.checkBGADir(path, bg.fileName, basePath);
                                if (bgaDir != null) {
                                    player.setBackground(new BitmapDrawable(BitmapFactory.decodeFile(bgaDir)));
                                } else
                                    //no se utilizo owo

                                    playBgaOff();
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    changeVideo = false;
                }
            }
            if (beat > BGList.get(currentBg).beat) {
                currentBg++;
                changeVideo = true;
            }
        }
    }

    private void playBgaOff() {
        String path2 = "android.resource://" + context.getPackageName() + "/" + R.raw.bgaoff;
        player.setVideoPath(path2);
        player.start();
    }

    public void start(double beat) {
        if (BGList.size() == 0) {
            playBgaOff();
        } else {
            isRunning = true;
            changeVideo = true;
            update(beat);
        }
    }

    class bgChange {
        private float beat, prop1;
        private String fileName;
        private byte prop2, prop3, prop4;

        bgChange(String data) {
            String[] info = data.split("=");
            if (info.length > 1) {
                try {
                    beat = Float.parseFloat(info[0]);
                    fileName = info[1];
                    prop1 = Float.parseFloat(info[2]);
                    //prop2 = Byte.parseByte(info[3]);
                    //prop3 = Byte.parseByte(info[4]);
                    //prop4 = Byte.parseByte(info[5]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                try {
                    beat = -1;
                    if (info[0] != null && info[0] != "") {
                        fileName = info[0];
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        public float getBeat() {
            return beat;
        }
    }
}
