package org.neverscroll.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedDetectorTest {
    @Test fun youtubeShortsNeedsViewerEvidence() {
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE,
            ScreenSignals(setOf("youtube:id/reel_watch_fragment"), emptySet())))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE,
            ScreenSignals(emptySet(), setOf("shorts", "like", "comments"))))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE,
            ScreenSignals(emptySet(), setOf("shorts"), rightRailActions =
                setOf(VideoAction.LIKE, VideoAction.COMMENT))))
    }

    @Test fun revancedDirectShortUsesYoutubeRules() {
        assertTrue(ProtectedApp.fromPackage("app.revanced.android.youtube") ==
            ProtectedApp.YOUTUBE_REVANCED)
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(emptySet(), setOf("shorts"), rightRailActions =
                setOf(VideoAction.LIKE, VideoAction.COMMENT))))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(emptySet(), setOf("shorts"))))
    }

    @Test fun revancedSubscriptionsShortsFilterAndPostActionsAreNotAShort() {
        val screen = Bounds(0, 0, 1272, 2772)
        val actions = FeedDetector.verticalRailActions(screen, listOf(
            Bounds(12, 2262, 131, 2333) to "Нравится",
            Bounds(974, 2262, 1141, 2333) to "Поделиться",
            Bounds(1153, 2262, 1272, 2333) to "Оставить комментарий",
        ))
        assertTrue(actions.isEmpty())
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(setOf("app.revanced.android.youtube:id/pivot_bar",
                "app.revanced.android.youtube:id/reel_time_bar"),
                setOf("shorts", "нравится", "поделиться", "оставить комментарий"),
                setOf("подписки"), actions)))
    }

    @Test fun revancedSubscriptionsShortStaysProtectedWhenPlaybackHidesShortsTitle() {
        val rail = setOf(VideoAction.LIKE, VideoAction.COMMENT, VideoAction.SHARE)
        val playerIds = setOf("app.revanced.android.youtube:id/reel_player_page_container")
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(playerIds, emptySet(), rightRailActions = rail)))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(setOf("app.revanced.android.youtube:id/reel_time_bar"),
                emptySet(), rightRailActions = rail)))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(playerIds, emptySet())))
    }

    @Test fun shortsActionsFormAVerticalRail() {
        val actions = FeedDetector.verticalRailActions(Bounds(0, 0, 1272, 2772), listOf(
            Bounds(1090, 1530, 1200, 1640) to "Like",
            Bounds(1090, 1710, 1200, 1820) to "Comments",
            Bounds(1090, 1890, 1200, 2000) to "Share",
        ))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.YOUTUBE_REVANCED,
            ScreenSignals(emptySet(), setOf("shorts"), rightRailActions = actions)))
    }

    @Test fun instagramHomePostIsNotReel() {
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.INSTAGRAM,
            ScreenSignals(emptySet(), setOf("reels", "like", "comment"))))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.INSTAGRAM,
            ScreenSignals(setOf("instagram:id/reels_tab"), setOf("reels", "like", "comment"))))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.INSTAGRAM,
            ScreenSignals(setOf("instagram:id/reel_viewer"), emptySet())))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.INSTAGRAM,
            ScreenSignals(emptySet(), setOf("like"), setOf("Reels".lowercase()))))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.INSTAGRAM,
            ScreenSignals(emptySet(), emptySet(), rightRailActions =
                setOf(VideoAction.LIKE, VideoAction.COMMENT))))
    }

    @Test fun tiktokNeedsFeedAndEngagement() {
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.TIKTOK,
            ScreenSignals(emptySet(), setOf("for you", "like"))))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.TIKTOK,
            ScreenSignals(emptySet(), setOf("for you"))))
        assertTrue(FeedDetector.isShortFeed(ProtectedApp.TIKTOK,
            ScreenSignals(emptySet(), emptySet(), rightRailActions =
                setOf(VideoAction.COMMENT, VideoAction.SHARE))))
        assertFalse(FeedDetector.isShortFeed(ProtectedApp.TIKTOK,
            ScreenSignals(emptySet(), emptySet(), rightRailActions = setOf(VideoAction.LIKE))))
    }

    @Test fun rightRailRejectsHorizontalPostActions() {
        val screen = Bounds(0, 0, 1080, 2400)
        assertTrue(FeedDetector.rightRailAction(screen, Bounds(912, 1432, 1080, 1632),
            "view 1,500 comments") == VideoAction.COMMENT)
        assertTrue(FeedDetector.rightRailAction(screen, Bounds(70, 2100, 210, 2240),
            "like") == null)
        assertTrue(FeedDetector.rightRailAction(screen, Bounds(912, 1432, 1080, 1632),
            "dislike") == null)
    }

    @Test fun vkAppsHaveSeparateProtection() {
        assertTrue(ProtectedApp.fromPackage("com.vkontakte.android") == ProtectedApp.VK)
        assertTrue(ProtectedApp.fromPackage("com.vk.vkvideo") == ProtectedApp.VK_VIDEO)
        assertTrue(ProtectedApp.VK.preferenceKey != ProtectedApp.VK_VIDEO.preferenceKey)
        assertTrue(ProtectedApp.fromPackage("com.vk.clips") == null)
    }

    @Test fun vkAndVkVideoClipsUseViewerAndVerticalControlsWithoutFeedTitle() {
        // VK 8.199 and VK Video 1.166 on a 1272 x 2772 Android 16 phone.
        for ((app, pkg) in listOf(ProtectedApp.VK to "com.vkontakte.android",
                ProtectedApp.VK_VIDEO to "com.vk.vkvideo")) {
            val rail = FeedDetector.verticalRailActions(Bounds(0, 0, 1272, 2772), listOf(
                Bounds(1105, 1642, 1248, 1785) to "$pkg:id/fullscreen_clip_overlay_like_image",
                Bounds(1105, 1832, 1248, 1975) to "Комментарий",
                Bounds(1105, 2022, 1248, 2165) to "Поделиться",
            ))
            assertTrue(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("$pkg:id/fullscreen_clip_container"), emptySet(), rightRailActions = rail)))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("$pkg:id/fullscreen_clip_container"), emptySet())))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("$pkg:id/clip_feed_seekbar"), emptySet(), rightRailActions = rail)))
        }
    }

    @Test fun vkTabsHomePostsAndLongVideosAreNotClips() {
        for (app in listOf(ProtectedApp.VK, ProtectedApp.VK_VIDEO)) {
            val postActions = FeedDetector.verticalRailActions(Bounds(0, 0, 1272, 2772), listOf(
                Bounds(48, 2000, 191, 2143) to "Нравится",
                Bounds(239, 2000, 382, 2143) to "Комментарий",
                Bounds(430, 2000, 573, 2143) to "Поделиться",
            ))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("tab_clips", "clip_feed_background", "clip_feed_seekbar_background"),
                setOf("клипы", "clips", "нравится", "комментарий", "поделиться"),
                setOf("клипы"), postActions)))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("fullscreen_video_base_video_container", "video_player"),
                setOf("нравится", "комментарий", "поделиться"), rightRailActions =
                    setOf(VideoAction.LIKE, VideoAction.COMMENT, VideoAction.SHARE))))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("comment_bottom_sheet_background", "bottom_sheet_container"),
                setOf("комментарии", "нравится"))))
            assertFalse(FeedDetector.isShortFeed(app, ScreenSignals(
                setOf("fullscreen_clip_container_thumbnail"), emptySet(), rightRailActions =
                    setOf(VideoAction.LIKE, VideoAction.COMMENT))))
        }
    }

    @Test fun onlyKnownPackagesAreInspected() {
        assertTrue(ProtectedApp.fromPackage("com.google.android.youtube") == ProtectedApp.YOUTUBE)
        assertTrue(ProtectedApp.fromPackage("com.zhiliaoapp.musically") == ProtectedApp.TIKTOK)
        assertTrue(ProtectedApp.fromPackage("org.telegram.messenger") == null)
    }
}
