package com.cobrachris.sanftwecker;

/** Only accept direct APK assets attached to Sanftwecker GitHub releases. */
public final class UpdateUrlPolicy {
    private static final String PREFIX =
            "https://github.com/CobraChris/app-fabrik-template/releases/download/";
    private static final String NEW_PREFIX =
            "https://github.com/CobraChris/sanftwecker/releases/download/";
    private static final String FILE="/sanftwecker-release.apk";
    public static final String OLD_REPO="CobraChris/app-fabrik-template";
    public static final String NEW_REPO="CobraChris/sanftwecker";

    private UpdateUrlPolicy(){}

    public static long versionCode(String tag){
        if(tag==null||!tag.matches("sanftwecker-v[1-9][0-9]{0,9}"))return -1;
        try {long code=Long.parseLong(tag.substring("sanftwecker-v".length()));
            return code<=2_100_000_000L ? code : -1;}
        catch(NumberFormatException e){return -1;}
    }
    /** Only the two explicitly approved GitHub release feeds are trusted. */
    public static boolean isAllowed(String url){
        return matchesRepo(url,OLD_REPO)||matchesRepo(url,NEW_REPO);
    }
    private static boolean matchesRepo(String url,String repo){
        String base=OLD_REPO.equals(repo)?PREFIX:NEW_REPO.equals(repo)?NEW_PREFIX:null;
        if(base==null||url==null||!url.startsWith(base)||!url.endsWith(FILE))return false;
        String tag=url.substring(base.length(),url.length()-FILE.length());
        return versionCode(tag)>0 && url.equals(base+tag+FILE);
    }
    /** GitHub API metadata must point to an asset under the SAME release tag. */
    public static boolean isForTag(String url,String tag){
        return isForTagFromRepository(url,tag,OLD_REPO)||
                isForTagFromRepository(url,tag,NEW_REPO);
    }
    /** Never accept an asset for one repository in another repository's API response. */
    public static boolean isForTagFromRepository(String url,String tag,String repo){
        if(!OLD_REPO.equals(repo)&&!NEW_REPO.equals(repo))return false;
        String base=OLD_REPO.equals(repo)?PREFIX:NEW_PREFIX;
        return versionCode(tag)>0 && matchesRepo(url,repo) &&
                url.equals(base+tag+FILE);
    }
    public static boolean isNewer(long published,long installed){
        return published>0&&published>installed;
    }
}
