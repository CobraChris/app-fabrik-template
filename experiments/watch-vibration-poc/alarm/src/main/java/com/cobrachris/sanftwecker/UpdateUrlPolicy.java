package com.cobrachris.sanftwecker;

/** Only accept direct APK assets attached to Sanftwecker GitHub releases. */
public final class UpdateUrlPolicy {
    private static final String PREFIX =
            "https://github.com/CobraChris/app-fabrik-template/releases/download/";
    private static final String FILE="/sanftwecker-release.apk";

    private UpdateUrlPolicy(){}

    public static long versionCode(String tag){
        if(tag==null||!tag.matches("sanftwecker-v[0-9]{1,8}"))return -1;
        try {return Long.parseLong(tag.substring("sanftwecker-v".length()));}
        catch(NumberFormatException e){return -1;}
    }
    public static boolean isAllowed(String url){
        if(url==null||!url.startsWith(PREFIX)||!url.endsWith(FILE))return false;
        String tag=url.substring(PREFIX.length(),url.length()-FILE.length());
        return versionCode(tag)>0;
    }
    public static boolean isNewer(long published,long installed){
        return published>0&&published>installed;
    }
}
