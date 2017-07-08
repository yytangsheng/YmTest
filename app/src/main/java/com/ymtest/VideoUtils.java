package com.ymtest;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 视频截取与合并  需要libs下的三个jar包
 */
public class VideoUtils {

    /**
     * 合并视频后的方向设定
     */
    static final long[] ROTATE_0 = new long[]{1, 0, 0, 1, 0, 0, 1, 0, 0};
    static final long[] ROTATE_90 = new long[]{0, 1, -1, 0, 0, 0, 1, 0, 0};
    static final long[] ROTATE_180 = new long[]{-1, 0, 0, -1, 0, 0, 1, 0, 0};
    static final long[] ROTATE_270 = new long[]{0, -1, 1, 0, 0, 0, 1, 0, 0};

    private VideoUtilsExecuteListener videoUtilsExecuteListener;

    /**
     * 设置监听
     * @param videoUtilsExecuteListener
     */
    public void setVideoUtilsExecuteListener(VideoUtilsExecuteListener videoUtilsExecuteListener){
        this.videoUtilsExecuteListener = videoUtilsExecuteListener;
    }

    /**
     * 裁剪视频
     *      当剪辑设置的时长比源视频长时，会自动设置为最大值
     *      当startTime大于源视频的时间时，会报错
     * @param mediaPath  被裁剪的视频路径
     * @param startTime  裁剪的开始时间 单位：秒
     * @param length  裁剪的时间长度 单位：秒  endTime = startTime + length
     */
    public void videoTrimm(String mediaPath, int startTime, int length){
        new TrimmVideoAsyncTask(mediaPath,startTime,length).execute();
    }

    /**
     * 裁剪视频的异步任务
     */
    private class TrimmVideoAsyncTask extends AsyncTask<Void, Void, String> {
        /**
         * 被裁剪的视频路径(秒)
         */
        private String mediaPath;

        /**
         * 开始的时间(秒)
         */
        private double startTime;

        /**
         * 结束的时间(秒)
         */
        private double endTime;

        /**
         * 截取的时间长度(秒)
         */
        private int length;

        private TrimmVideoAsyncTask(String mediaPath, int startTime, int length) {
            this.mediaPath = mediaPath;
            this.startTime = startTime;
            this.length = length;
            this.endTime = this.startTime + this.length;
        }

        @Override
        protected void onPreExecute() {
            videoUtilsExecuteListener.onPreExecute();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            return trimVideo();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(!TextUtils.isEmpty(result)){
                videoUtilsExecuteListener.onPostExecute(result);
            }
        }

        /**
         * 开始裁剪
         */
        private String trimVideo() {
            try {
                File file = new File(mediaPath);
                FileInputStream fis = new FileInputStream(file);
                FileChannel in = fis.getChannel();
                Movie movie = MovieCreator.build(in);
                List<Track> tracks = movie.getTracks();
                movie.setTracks(new LinkedList<Track>());

                boolean timeCorrected = false;


                // 这里我们尝试找到一个有同步样本的轨迹。因为我们只能开始解码
                // 在这样的示例中，我们应该确保新片段的开始是正确的
                for (Track track : tracks) {
                    if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                        if (timeCorrected) {
                            throw new RuntimeException("startTime已经被另一个带有SyncSample的音轨更正了,不支持");
                        } else {
                            startTime = correctTimeToNextSyncSample(track, startTime);
                            timeCorrected = true;
                        }
                    }
                }

                for (Track track : tracks) {
                    long currentSample = 0;
                    double currentTime = 0;
                    long startSample = -1;
                    long endSample = -1;

                    for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                        TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                        for (int j = 0; j < entry.getCount(); j++) {
                            // entry.getDelta() 是当前样品所覆盖的时间。

                            if (currentTime <= startTime) {
                                //当前的样例仍然在新的开始时间之前
                                startSample = currentSample;
                            } else if (currentTime <= endTime) {
                                // 当前的样本是在新的开始时间之后，仍然在新的时间结束之前
                                endSample = currentSample;
                            } else {
                                // 当前的样本是在裁剪后的视频结束后
                                break;
                            }
                            currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                            currentSample++;
                        }
                    }
                    movie.addTrack(new CroppedTrack(track, startSample, endSample));
                }
                //if(startTime==length)
                //throw new Exception("times are equal, something went bad in the conversion");

                IsoFile out = new DefaultMp4Builder().build(movie);
                //这里用原视频的路径作为剪辑后的视频保存路径
                File storagePath = new File(mediaPath).getParentFile();
                storagePath.mkdirs();

                //File myMovie = new File(storagePath, String.format("output-%s-%f-%d.mp4", timestampS, startTime*1000, length*1000));
                File myMovie = new File(storagePath, getVideoFileName());
                //Log.e("tag","裁剪完成的视频保存路径= " + myMovie.getAbsolutePath());

                FileOutputStream fos = new FileOutputStream(myMovie);
                FileChannel fc = fos.getChannel();
                out.getBox(fc);

                fc.close();
                fos.close();
                fis.close();
                in.close();
                return myMovie.getAbsolutePath();
            } catch(Exception e){
                e.printStackTrace();
                videoUtilsExecuteListener.onerrorExecute(e);
            }
            return null;
        }

        private double correctTimeToNextSyncSample(Track track, double cutHere) {
            double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
            long currentSample = 0;
            double currentTime = 0;
            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                        // 样本总是从1开始但我们从0开始，因此+1
                        timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
                    }
                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }
            }
            for (double timeOfSyncSample : timeOfSyncSamples) {
                if (timeOfSyncSample > cutHere) {
                    return timeOfSyncSample;
                }
            }
            return timeOfSyncSamples[timeOfSyncSamples.length - 1];
        }
    }

    /**
     * 视频处理监听
     */
    public interface VideoUtilsExecuteListener{
        public void onPreExecute();//准备中
        public void onPostExecute(String filePath);//完成
        public void onerrorExecute(Exception e);//错误
    }








    //===============================视频的合并==============================================

    /**
     * 合并视频
     * @param mediaPath  合并后视频的保存路径(文件夹)
     * @param videosToMerge  需要合并的源视频路径
     */
    public void videoMerge(String mediaPath, ArrayList<String> videosToMerge){
        new MergeVideosAsyncTask(mediaPath, videosToMerge).execute();
    }


    /**
     * 合并视频
     */
    private class MergeVideosAsyncTask extends AsyncTask<String, Integer, String> {

        /**
         * 视频文件所在的工作路径(文件夹)
         */
        private String workingPath;

        /**
         * 合并的视频
         */
        private ArrayList<String> videosToMerge;

        private MergeVideosAsyncTask(String workingPath, ArrayList<String> videosToMerge) {
            this.workingPath = workingPath;
            this.videosToMerge = videosToMerge;
        }

        @Override
        protected void onPreExecute() {
            videoUtilsExecuteListener.onPreExecute();
        };

        @Override
        protected String doInBackground(String... params) {
            int count = videosToMerge.size();
            try {
                Movie[] inMovies = new Movie[count];
                for (int i = 0; i < count; i++) {
                    File file = new File(videosToMerge.get(i));
                    if(file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        inMovies[i] = MovieCreator.build(fc);
                        fis.close();
                        fc.close();
                    }
                }
                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();

                for (Movie m : inMovies) {
                    for (Track t : m.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTracks.add(t);
                        }
                        if (t.getHandler().equals("vide")) {
                            videoTracks.add(t);
                        }
                        if (t.getHandler().equals("")) {

                        }
                    }
                }

                Movie result = new Movie();

                if (audioTracks.size() > 0) {
                    result.addTrack(new AppendTrack(audioTracks
                            .toArray(new Track[audioTracks.size()])));
                }
                if (videoTracks.size() > 0) {
                    result.addTrack(new AppendTrack(videoTracks
                            .toArray(new Track[videoTracks.size()])));
                }
                IsoFile out = new DefaultMp4Builder()
                        .build(result);


                out.getMovieBox().getMovieHeaderBox().setMatrix(ROTATE_270);//设置合并后视频的方向

                File storagePath = new File(workingPath);
                storagePath.mkdirs();

                File myMovie = new File(storagePath, getVideoFileName());

                FileOutputStream fos = new FileOutputStream(myMovie);
                FileChannel fco = fos.getChannel();
                fco.position(0);
                out.getBox(fco);
                fco.close();
                fos.close();

                return myMovie.getAbsolutePath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                videoUtilsExecuteListener.onerrorExecute(e);
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                videoUtilsExecuteListener.onerrorExecute(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String value) {
            super.onPostExecute(value);
            if(!TextUtils.isEmpty(value)){
                videoUtilsExecuteListener.onPostExecute(value);
            }
        }

    }


    /**
     * 使用系统当前日期加以调整作为视频的名称
     * @return
     */
    private String getVideoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'MP4'_yyyyMMdd_HHmmss");
        System.out.println(dateFormat.format(date) + ".jpg");
        return dateFormat.format(date) + ".mp4";//输出结果(具体时间)：IMG_20160504_203209.jpg
    }
}
