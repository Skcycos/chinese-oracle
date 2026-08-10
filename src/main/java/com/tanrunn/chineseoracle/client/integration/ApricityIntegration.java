package com.tanrunn.chineseoracle.client.integration;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import com.tanrunn.chineseoracle.common.network.FortuneDisplay;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * ApricityUI integration (soft dependency). Opens the 黄历 as a fullscreen
 * ApricityScreen populated from the server-synced fortune. This class is only
 * ever loaded when the apricityui mod is installed.
 */
public final class ApricityIntegration {
    private ApricityIntegration() {
    }

    public static void openFortune(FortuneDisplay display) {
        Minecraft.getInstance().setScreen(new FortuneScreen(display)
                .setPauseGame(false)
                .setShowDefaultBackground(true));
    }

    private static final class FortuneScreen extends ApricityScreen {
        private final FortuneDisplay display;
        private boolean explainOpen;

        FortuneScreen(FortuneDisplay display) {
            super("screens/fortune.html");
            this.display = display;
        }

        @Override
        protected void init() {
            super.init();
            Document doc = getLinkedDocument();
            if (doc == null) return;

            setText(doc, "aui-title", "今日黄历");
            Element tier = doc.getElementById("aui-tier");
            if (tier != null) {
                tier.setTextContent(display.tierName());
                tier.setAttribute("class", "tier " + tierColor(display.tierRank()));
            }
            Element seal = doc.getElementById("aui-seal");
            if (seal != null) {
                seal.setTextContent(sealText(display.tierName()));
                seal.setAttribute("class", "seal " + sealSizeClass(display.tierName()));
            }
            setBadge(doc, "aui-wuxing", display.wuxing() == null ? "" : "五行 " + display.wuxing());
            setBadge(doc, "aui-shichen", display.shichen() + (display.shichenAuspicious() ? "（吉）" : ""));
            setBadge(doc, "aui-solar", display.solarTerm());
            setBadge(doc, "aui-festival", display.festival());
            setText(doc, "aui-yi", join(display.yiNames()));
            setText(doc, "aui-ji", join(display.jiNames()));
            setText(doc, "aui-poem", display.poem() == null ? "" : display.poem());
            setText(doc, "aui-explain", display.explain() == null ? "" : display.explain());
            setToggleVisibility(doc, display.explain());
            setText(doc, "aui-day", "第 " + display.dayIndex() + " 日");

            bindToggle(doc);
        }

        private void bindToggle(Document doc) {
            Element toggle = doc.getElementById("aui-toggle");
            if (toggle == null) return;
            toggle.addEventListener("click", event -> {
                explainOpen = !explainOpen;
                Element explain = doc.getElementById("aui-explain");
                if (explain != null) {
                    explain.setAttribute("class", explainOpen ? "explain open" : "explain");
                }
                toggle.setTextContent(explainOpen ? "收起解签" : "展开解签");
            });
        }

        private static String tierColor(int rank) {
            if (rank >= 5) return "good";
            if (rank == 4) return "mid";
            return "bad";
        }

        private static String sealText(String tierName) {
            if (tierName == null) return "";
            if (tierName.length() >= 4) {
                return tierName.substring(0, 2) + "\n" + tierName.substring(2, 4);
            }
            return tierName;
        }

        private static String sealSizeClass(String tierName) {
            return tierName != null && tierName.length() <= 2 ? "seal-single" : "seal-multi";
        }

        private static String join(List<String> items) {
            return items.isEmpty() ? "暂无" : String.join("、", items);
        }

        private static void setText(Document doc, String id, String text) {
            Element el = doc.getElementById(id);
            if (el != null) {
                el.setTextContent(text);
            }
        }

        private static void setBadge(Document doc, String id, String text) {
            Element el = doc.getElementById(id);
            if (el == null) return;
            if (text == null || text.isBlank()) {
                el.setAttribute("style", "display:none");
            } else {
                el.setTextContent(text);
            }
        }

        private static void setToggleVisibility(Document doc, String explain) {
            Element toggle = doc.getElementById("aui-toggle");
            if (toggle == null) return;
            toggle.setAttribute("style", explain == null || explain.isBlank()
                    ? "display:none"
                    : "display:inline-block");
        }
    }
}
