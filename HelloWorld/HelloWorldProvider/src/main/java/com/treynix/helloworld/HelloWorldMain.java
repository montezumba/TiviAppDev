package com.treynix.helloworld;


import android.util.SparseArray;

import com.montezumba.lib.io.StorageHandler;
import com.montezumba.lib.types.AddonRequest;
import com.montezumba.lib.types.AddonResponse;
import com.montezumba.lib.types.Constants;
import com.montezumba.lib.types.HTTPVideoTitle;
import com.montezumba.lib.types.MediaLog;
import com.montezumba.lib.types.TimeStamp;
import com.montezumba.lib.types.TransportableTitle;
import com.montezumba.lib.types.TvGuide;
import com.montezumba.lib.types.exceptions.AddonException;
import com.montezumba.lib.types.exceptions.WorkerException;
import com.montezumba.lib.utils.FileReader;
import com.montezumba.lib.utils.FileWriter;
import com.montezumba.lib.utils.SimpleBackgroundTask;
import com.montezumba.lib.utils.TimerFactory;
import com.montezumba.lib.utils.WorkerFactory;
import com.montezumba.lib.utils.WorkerFactory.Worker;
import com.treynix.tiviapplive.provider.AddonHandler;
import com.treynix.tiviapplive.provider.AddonMain;
import com.treynix.tiviapplive.provider.config.AddonConfig;

import java.io.IOException;
import java.util.ArrayList;

import static com.montezumba.lib.types.Constants.DAYS;


public class HelloWorldMain extends AddonMain {


    private Worker mPlaylistWorker =
            WorkerFactory.instance().createWorker("DemoProvider-Playlist", WorkerFactory.WorkerType.ONE_TIME_WORKER);
    private Worker mLiveUrlWorker =
            WorkerFactory.instance().createWorker("DemoProvider-Live", WorkerFactory.WorkerType.PERSISTENT_WORKER);
    private Worker mTvGuideWorker =
            WorkerFactory.instance().createWorker("DemoProvider-Guide", WorkerFactory.WorkerType.ONE_TIME_WORKER);
    private static final String XMLTV_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tv>\n";
    private static final String XMLTV_FOOTER = "</tv>";
    private static final String[] sDays = {

            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
    };
    private static String sLastGeneratedDate = null;
    private static TimeStamp sDates[] = new TimeStamp[7];


    /**
     * This method is invoked whenever the server (TiviApp) requests some information from this provider.
     * This method runs on the main (UI) thread of your application, so if you have a heavy logic that needs to performed, you should use a {@link com.montezumba.lib.utils.WorkerFactory.Worker} to execute it on another thread.
     * When you ready to send the response back to the server, you should call the following two methods:
     * <ul>
     *     <li>Call {@link AddonHandler#sendIndexerResponse(AddonResponse)} to send the results back to the server. NOTE: The server won't process them until you call {@link #done()}</li>
     *     <li>Call {@link #done()} to terminate the execution of the current request and start processing the results on the server's side. The method should return immediately after calling this.</li>
     * </ul>
     * Each request has the following runtime restrictions:
     * <ul>
     *     <li>A local limit as defined by {@link AddonConfig#MAX_ADDON_EXEC_TIME}: Imposes the maximal allowed time for the execution of a single request. Even though you may configure this value upon the initialization of this class, it is still recommended to set this value as low as possible to allow the system to detect potential deadlocks.</li>
     *     <li>A global limit that is defined by the sever (TiviApp), which requires that all requests will be completed within 1 minute.</li>
     * </ul>
     * @param params a set of parameters for this request.
     * @throws AddonException should be thrown for any exception that is found during the execution of this method.
     */
    @Override
    protected void onStart(final AddonRequest params) throws AddonException {

        final AddonResponse result = new AddonResponse();
        result.requestId = params.requestId;
        result.mediaResults = new ArrayList<TransportableTitle>();
        switch(params.procName) {

            case "request_tvguide":

                mTvGuideWorker.run(new SimpleBackgroundTask() {
                    @Override
                    protected void doInBackground() throws WorkerException {

                        try {
                            // Build DEMO 1 Guide
                            TvGuide guide1 = new TvGuide("Demo TV Guide 1");
                            String guidePath = StorageHandler.instance().getAppStoragePath();
                            guidePath = StorageHandler.instance().generateFullPath(guidePath, HelloWorldConfig.DEMO_GUIDE_1);
                            guide1.pathToGuide = guidePath;
                            if (needToRefreshGuide(guide1)) {
                                generateDemo1TvGuide();
                                guide1.startDate = TimerFactory.instance().getCurrentTime().toLocalTime();
                                guide1.endDate = TimerFactory.instance().getCurrentTime().toLocalTime();
                                guide1.endDate.addTime(HelloWorldConfig.MAX_TVGUIDE_DAYS * Constants.DAYS);
                            }
                            result.mediaResults.add(guide1.toTrasportableTitle());

                        } catch(IOException e) {
                            e.printStackTrace();
                        }

                        if(result.mediaResults.isEmpty()) {
                            AddonHandler.instance().sendError(params, "Failed to generate TV Guide");
                            done();
                            return;
                        }
                        else {
                            MediaLog.instance().debug("Sending tv guide...");
                            done();
                            AddonHandler.instance().sendIndexerResponse(result);
                        }
                    }
                });

                break;


            case "request_live_playlist":

                mPlaylistWorker.run(new SimpleBackgroundTask() {
                    @Override
                    protected void doInBackground() throws WorkerException {
                        for (HelloWorldConfig.PlaylistSource source : HelloWorldConfig.PlaylistSource.values()) {
                            result.mediaResults.add(source.getPlaylist().toTrasportableTitle());
                        }
                        done();
                        AddonHandler.instance().sendIndexerResponse(result);
                    }
                });

                break;

            case "request_live_url":
                mLiveUrlWorker.run(new SimpleBackgroundTask() {

                    @Override
                    protected void doInBackground() throws WorkerException {
                        String siteParams[] = params.query.split("/");
                        String site = siteParams[0];


                        if (siteParams.length < 2) {
                            AddonHandler.instance().sendError(params, "Bad params");
                            done();
                            return;
                        }

                        String channel = siteParams[1];
                        MediaLog.instance().debug("Got channel request = " + channel);
                        int channelId = Integer.parseInt(channel);
                        SparseArray<String> siteChannels = HelloWorldConfig.ENCODED_CHANNELS.get(site);
                        if(siteChannels == null) {
                            AddonHandler.instance().sendError(params, "Cannot find source with name="+site);
                            done();
                            return;
                        }

                        String decodedUrl = siteChannels.get(channelId);

                        if(decodedUrl == null) {
                            AddonHandler.instance().sendError(params, "Cannot find channel id="+channelId+" for source="+site);
                            done();
                            return;
                        }

                        HTTPVideoTitle title = new HTTPVideoTitle("Decoded Channel:"+channelId);
                        title.url = decodedUrl;
                        result.mediaResults.add(title.toTrasportableTitle());
                        AddonHandler.instance().sendIndexerResponse(result);
                        done();
                    }
                });

                break;

            default:
                AddonHandler.instance().sendError(params, "Not-supported procedure - "+params.procName);
                done();
        }

    }

    /**
     * This method is invoked upon the initialization of this provider.
     * You may use this method to perform any general initializations that do not depend on any specific request from TiviApp.<br/>
     * NOTE: This method is blocking. To avoid starvation of pending requests from the server you should use {@link com.montezumba.lib.utils.WorkerFactory.Worker} for time-consuming logic.
     * @throws AddonException should be thrown for any exception that is found during the execution of this method.
     */
    @Override
    protected void onInit() throws AddonException {}

    /**
     * This method is invoked upon the destruction of this provider.
     * You may use this method to perform wrap-up logic such as: stopping pending data transactions, closing files, cleaning-up memory and etc.<br/>
     * NOTE: This method is blocking. To avoid starvation of pending requests from the server you should use {@link com.montezumba.lib.utils.WorkerFactory.Worker} for time-consuming logic.
     * @throws AddonException should be thrown for any exception that is found during the execution of this method.
     */
    @Override
    protected void onDestroy() {}

    /**
     * This method is invoked when the server (TiviApp) requests this provider to terminate its currently-processed request.
     * You should use this method to cancel any pending activities and ensure that this provide is ready again to receive new requests from TiviApp.<br/>
     * NOTE: This method is blocking. To avoid starvation of pending requests from the server you should use {@link com.montezumba.lib.utils.WorkerFactory.Worker} for time-consuming logic.
     * @throws AddonException should be thrown for any exception that is found during the execution of this method.
     */
    @Override
    protected void onCancel() {}

    /**
     * This method is invoked when the server (TiviApp) requests to clone
     * @return
     */
    @Override
    protected HelloWorldMain clone() {
        return new HelloWorldMain();
    }







    private static boolean needToRefreshGuide(TvGuide guide) throws IOException {

        String guidePath = guide.pathToGuide;
        if(StorageHandler.instance().isExist(guidePath)) {
            FileReader reader = null;
            try {
                reader = StorageHandler.instance().openFile(guidePath);
                TimeStamp lastModified = reader.getLastModified();
                guide.startDate = TimerFactory.instance().createTimeStamp(lastModified.getEpoch());
                TimeStamp currentTime = TimerFactory.instance().getCurrentTime().toLocalTime();
                MediaLog.instance().debug("Current time = "+currentTime.toString());
                currentTime.addTime(-HelloWorldConfig.TVGUIDE_REFRESH_HOURS * Constants.HOURS);
                MediaLog.instance().debug("Current time = "+currentTime.toString()+" modified time="+lastModified.toString());
                if(currentTime.after(lastModified)) {
                    MediaLog.instance().debug("Need to refresh...");
                    //refresh = true;
                    return true;
                }
                currentTime = TimerFactory.instance().getCurrentTime().toLocalTime();
                currentTime.addTime(-HelloWorldConfig.MAX_TVGUIDE_DAYS * Constants.DAYS);
                if(currentTime.before(lastModified)) {
                    MediaLog.instance().debug("Tv guide is valid");
                    //valid = true;
                    lastModified.addTime(HelloWorldConfig.MAX_TVGUIDE_DAYS * Constants.DAYS);
                    guide.endDate = TimerFactory.instance().createTimeStamp(lastModified.getEpoch());
                    return false;
                }

            } catch (IOException e) {
                throw e;
            } finally {
                if(reader != null) {
                    reader.close();
                }
            }

        } else {
            MediaLog.instance().debug("Cannot find guide");
            //refresh = true;
            return true;
        }

        return true;
    }

    private static void generateDemo1TvGuide() throws IOException {
        String tempPath = null;
        FileReader reader = null;
        FileWriter writer = null;

        try {
            tempPath = StorageHandler.instance().getAppTempPath();
            String guidePath = StorageHandler.instance().getAppStoragePath();
            tempPath = StorageHandler.instance().generateFullPath(tempPath, HelloWorldConfig.XMLTV_TEMP_PATH);
            guidePath = StorageHandler.instance().generateFullPath(guidePath, HelloWorldConfig.DEMO_GUIDE_1);

            // read the fixed template:
            reader = StorageHandler.instance().openFile(HelloWorldConfig.DEMO_GUIDE_TEMPLATE);
            writer = StorageHandler.instance().openOutputFile(tempPath, false);
            String line = null;
            MediaLog.instance().debug("Start Generating");
            while ((line = reader.readNextLine()) != null) {
                line = setWeekDayAndZone(line, "America/Los_Angeles");
                writer.writeLine(line);
            }

            MediaLog.instance().debug("Finished Generating");
            MediaLog.instance().debug("Start Rename...");
            StorageHandler.instance().rename(tempPath, guidePath);
            MediaLog.instance().debug("End Rename...");

        } catch (IOException e) {
            e.printStackTrace();
            if(tempPath != null) {
                StorageHandler.instance().delete(tempPath);
            }
            throw e;
        } finally {
            if(writer != null) {
                writer.close();
                writer = null;
            }
            if(reader != null) {
                reader.close();
                reader = null;
            }
        }
    }

    private static String setWeekDayAndZone(String line, String offsetString) {

        int minutesOffset = TimerFactory.instance().getTimeZoneOffset(offsetString);

        TimeStamp currentTime = TimerFactory.instance().getCurrentTime().toLocalTime(minutesOffset);
        String currentDate = currentTime.format("%day%%month%%year%", false);

        // generate relatively to "yesterday" (to cover the case when the first show hasn't started yet)
        currentTime.addTime(-1 * DAYS);
        int currDay = currentTime.getWeekDay(false).ordinal();

        TimeStamp nextWeekTime = TimerFactory.instance().createTimeStamp(currentTime.getEpoch(), minutesOffset);
        nextWeekTime.addTime(7 * DAYS);
        String nextWeekDate = nextWeekTime.format("%year%%month%%day%", false);


        //MediaLog.instance().debug("Check if need to generate days: current="+currentDate+", previous="+ sLastGeneratedDate);
        if(sLastGeneratedDate == null || !sLastGeneratedDate.equals(currentDate)) {
            MediaLog.instance().debug("zone offset = "+minutesOffset);
            sDates = generateThisWeek(currentTime, minutesOffset);
            sLastGeneratedDate = currentDate;
        }

        String result =

                line.replace("@@%monday%@@",sDates[1].format("%year%%month%%day%", false)).
                        replace("@@%tuesday%@@",sDates[2].format("%year%%month%%day%", false)).
                        replace("@@%wednesday%@@",sDates[3].format("%year%%month%%day%", false)).
                        replace("@@%thursday%@@",sDates[4].format("%year%%month%%day%", false)).
                        replace("@@%friday%@@",sDates[5].format("%year%%month%%day%", false)).
                        replace("@@%saturday%@@",sDates[6].format("%year%%month%%day%", false)).
                        replace("@@%sunday%@@",sDates[0].format("%year%%month%%day%", false)).

                        replace("@@%nextmonday%@@",(currDay == 1) ? nextWeekDate : sDates[1].format("%year%%month%%day%", false)).
                        replace("@@%nexttuesday%@@",(currDay == 2) ? nextWeekDate : sDates[2].format("%year%%month%%day%", false)).
                        replace("@@%nextwednesday%@@",(currDay == 3) ? nextWeekDate : sDates[3].format("%year%%month%%day%", false)).
                        replace("@@%nextthursday%@@",(currDay == 4) ? nextWeekDate : sDates[4].format("%year%%month%%day%", false)).
                        replace("@@%nextfriday%@@",(currDay == 5) ? nextWeekDate : sDates[5].format("%year%%month%%day%", false)).
                        replace("@@%nextsaturday%@@",(currDay == 6) ? nextWeekDate : sDates[6].format("%year%%month%%day%", false)).
                        replace("@@%nextsunday%@@",(currDay == 0) ? nextWeekDate : sDates[0].format("%year%%month%%day%", false)).
                        replace("@@%PTZ%@@",generateZoneString(minutesOffset));

        return result;
    }

    private static String generateZoneString(int zoneOffset) {

        String zoneString = "";
        if(zoneOffset != 0) {
            int zoneHours = zoneOffset / 60;
            String zoneHoursStr = String.valueOf(Math.abs(zoneHours));
            String zoneSign = (zoneOffset > 0) ? "+" : "-";
            if (zoneHours < 10) {
                zoneHoursStr = "0" + zoneHoursStr;
            }
            int zoneMinutes = zoneOffset % 60;
            String zoneMinutesStr = String.valueOf(Math.abs(zoneMinutes));
            if (zoneMinutes < 10) {
                zoneMinutesStr = "0" + zoneMinutesStr;
            }
            zoneString = zoneSign + zoneHoursStr + zoneMinutesStr;
        }

        MediaLog.instance().debug("Zone string is: "+zoneString);

        return zoneString;
    }

    private static TimeStamp[] generateThisWeek(TimeStamp today, int zoneOffset) {

        // build dates table:
        // 0 - sunday
        // 6 - saturday

        TimeStamp dates[] = new TimeStamp[7];
        int daysOffset;
        //today.addTime(-daysOffset * Constants.DAYS);
        for (int i = 0; i < 7; ++i) {
            if (i < today.getWeekDay(false).ordinal()) {
                daysOffset = 7 - today.getWeekDay(false).ordinal() + i;
            } else {
                daysOffset = i - today.getWeekDay(false).ordinal();
            }

            dates[i] = TimerFactory.instance().createTimeStamp(today.getEpoch(), zoneOffset);
            //today.addTime(1 * Constants.DAYS);
            dates[i].addTime(daysOffset * Constants.DAYS);
            MediaLog.instance().debug("Date for " + TimeStamp.WeekDay.values()[i] + " is " + dates[i].format("%day%-%month%-%year%", false));
        }

        return dates;
    }







}
