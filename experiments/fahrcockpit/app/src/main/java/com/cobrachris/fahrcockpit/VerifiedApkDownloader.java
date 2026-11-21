package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * User-initiated APK download, SHA-256 and installed signing-certificate check.
 *
 * Must run on a worker thread. Only provides a read-only content URI; Android's
 * system package installer requires a separate, visible user confirmation.
 * No root access, no silent updates, no package deletion, no private signing keys.
 */
public final class VerifiedApkDownloader {
    private static final long MAX_BYTES=180L*1024*1024;
    private static final int MAX_REDIRECTS=5;
    private static final String APK_FILE="fahrcockpit-release.apk";
    private static final String MIME="application/vnd.android.package-archive";

    public interface Progress { void onProgress(int percent); }
    private VerifiedApkDownloader(){}

    public static String mimeType(){return MIME;}

    public static Uri download(
            Context context,String url,String sha256,long expectedCode,Progress progress)
            throws Exception {
        if(!VerifiedUpdatePolicy.isApproved(url,"sha256:"+sha256,expectedCode,
                installedCode(context)))
            throw new SecurityException("Updatequelle, Prüfsumme oder Versionsnummer ungültig");

        File dir=new File(context.getCacheDir(),"updates");
        if(!dir.isDirectory()&&!dir.mkdirs())
            throw new IllegalStateException("Update-Speicher nicht verfügbar");

        File temporary=new File(dir,APK_FILE+".part");
        File ready=new File(dir,APK_FILE);
        if(!temporary.delete()&&temporary.exists())
            throw new IllegalStateException("Alte unvollständige APK kann nicht entfernt werden");
        int notified=-1;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        long received=0;
        try{
            String next=url;
            HttpURLConnection conn=null;
            for(int redirect=0;redirect<=MAX_REDIRECTS;redirect++){
                if(!VerifiedUpdatePolicy.isTrustedDownloadHop(next))
                    throw new SecurityException("Unbekannter APK-Download-Host");
                conn=(HttpURLConnection)new URL(next).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent","FahrCockpit-Verified-Update");
                int status=conn.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String location=conn.getHeaderField("Location");
                    if(location==null)throw new SecurityException("APK-Weiterleitung ohne Ziel");
                    String resolved=new URL(new URL(next),location).toString();
                    conn.disconnect();
                    conn=null;
                    next=resolved;
                    continue;
                }
                if(status!=200)
                    throw new IllegalStateException("APK-Download HTTP "+status);
                break;
            }
            if(conn==null)throw new SecurityException("Zu viele APK-Weiterleitungen");
            try {
                long total=conn.getContentLengthLong();
                if(total>MAX_BYTES)
                    throw new SecurityException("APK überschreitet Größenbegrenzung");
                try(InputStream stream=conn.getInputStream();
                    FileOutputStream out=new FileOutputStream(temporary,false)){
                    byte[] buffer=new byte[16384];
                    int read;
                    while((read=stream.read(buffer))!=-1){
                        received+=read;
                        if(received>MAX_BYTES)
                            throw new SecurityException("APK überschreitet Größenbegrenzung");
                        digest.update(buffer,0,read);
                        out.write(buffer,0,read);
                        if(total>0&&progress!=null){
                            int percent=(int)Math.min(99,(100*received)/total);
                            if(percent>=notified+5){
                                notified=percent;
                                progress.onProgress(percent);
                            }
                        }
                    }
                    out.getFD().sync();
                }
            }finally{conn.disconnect();}
            if(received<=0)throw new SecurityException("Leere APK-Antwort");
            String actual=toHex(digest.digest());
            if(!actual.equals(sha256))
                throw new SecurityException("APK-Prüfsumme stimmt nicht mit GitHub überein");
            Files.move(temporary.toPath(),ready.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            try {
                verifyPackageAndCertificate(context,ready,expectedCode);
            }catch(Exception error) {
                ready.delete();
                throw error;
            }
            if(progress!=null)progress.onProgress(100);
            return FileProvider.getUriForFile(
                    context,context.getPackageName()+".updates",ready);
        }finally{temporary.delete();}
    }

    private static long installedCode(Context context) throws Exception {
        PackageInfo self=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
        return self.getLongVersionCode();
    }

    private static void verifyPackageAndCertificate(
            Context context,File apk,long expectedCode) throws Exception {
        PackageManager pm=context.getPackageManager();
        int flags=PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo downloaded=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        PackageInfo installed=pm.getPackageInfo(context.getPackageName(),flags);
        if(downloaded==null||
                !context.getPackageName().equals(downloaded.packageName)||
                downloaded.getLongVersionCode()!=expectedCode||
                downloaded.signingInfo==null||installed.signingInfo==null)
            throw new SecurityException("APK-Paket, Versionsnummer oder Signatur ungültig");
        Signature[] current=installed.signingInfo.getApkContentsSigners();
        Signature[] incoming=downloaded.signingInfo.getApkContentsSigners();
        if(current==null||incoming==null||current.length!=1||incoming.length!=1||
                !Arrays.equals(current[0].toByteArray(),incoming[0].toByteArray()))
            throw new SecurityException("APK wurde nicht mit dem Originalzertifikat signiert");
        if(!UpdateUrlPolicy.isNewer(expectedCode,installed.getLongVersionCode()))
            throw new SecurityException("Die APK wäre kein neueres Update");
    }

    private static String toHex(byte[] input){
        char[] chars="0123456789abcdef".toCharArray();
        char[] result=new char[input.length*2];
        for(int i=0;i<input.length;i++){
            int v=input[i]&0xff;
            result[2*i]=chars[v>>>4];
            result[2*i+1]=chars[v&15];
        }
        return new String(result);
    }
}
