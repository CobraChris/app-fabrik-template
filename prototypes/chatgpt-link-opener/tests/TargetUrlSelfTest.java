package io.github.cobrachris.chatlinkopener;

public final class TargetUrlSelfTest {
    private static void accept(String url) {
        if (TargetUrl.parse(url) == null) throw new AssertionError("Expected valid: " + url);
    }
    private static void reject(String url) {
        if (TargetUrl.parse(url) != null) throw new AssertionError("Expected invalid: " + url);
    }
    public static void main(String[] args) {
        accept("https://www.startpage.com/");
        accept("http://example.org/a?x=1&y=2");
        accept("https://example.com/%E6%97%A5%E6%9C%AC");
        reject(null);
        reject("");
        reject("javascript:alert(1)");
        reject("intent://example/#Intent;scheme=https;end");
        reject("file:///sdcard/private");
        reject("https://user:password@example.com/");
        reject("https://cobrachris.github.io/open/?url=hello");
        reject("https://");
        reject("https://example.com/space here");
        reject("https://" + "x".repeat(5000) + ".test/");
        System.out.println("TargetUrl: 13 checks passed");
    }
}
