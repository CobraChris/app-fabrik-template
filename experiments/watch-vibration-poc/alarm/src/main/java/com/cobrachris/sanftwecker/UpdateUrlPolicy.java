package com.cobrachris.sanftwecker;

/** Only accept direct APK assets attached to Sanftwecker GitHub releases. */
public final class UpdateUrlPolicy {
    private static final String PREFIX =
            "https://github.com/CobraChris/app-fabrik-template/releases/download/";
    private static final String FILE="/sanftwecker-release.apk";

    private UpdateUrlPolicy(){}

    public static long versionCode(String tag){
        if(tag==null||!tag.matches("sanftwecker-v[1-9][0-9]{0,9}"))return -1;
        try {long code=Long.parseLong(tag.substring("sanftwecker-v".length()));
            return code<=2_100_000_000L ? code : -1;}
        catch(NumberFormatException e){return -1;}
    }
    public static boolean isAllowed(String url){
        if(url==null||!url.startsWith(PREFIX)||!url.endsWith(FILE))return false;
        String tag=url.substring(PREFIX.length(),url.length()-FILE.length());
        return versionCode(tag)>0;
    }
    /** GitHub API metadata must point to an asset under the SAME release tag. */
    public static boolean isForTag(String url,String tag){
        return versionCode(tag)>0 && isAllowed(url) &&
                url.equals(PREFIX+tag+FILE);
    }
    public static boolean isNewer(long published,long installed){
        return published>0&&published>installed;
    }
}
