/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import javax.swing.*;
import java.awt.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public class Main {
  public static void main(String[] args) {
    Service s = new Service("Poland");
    String weatherJson = s.getWeather("Warsaw");
    Double rate1 = s.getRateFor("USD");
    Double rate2 = s.getNBPRate();
    // ...
    // część uruchamiająca GUI
      SwingUtilities.invokeLater(() -> gui(s, weatherJson, rate1, rate2));
  }
    private static void gui(Service s, String w, Double r1, Double r2) {
        JFrame f = new JFrame(":3");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1200, 800);

        JPanel panelGlowny = new JPanel(new BorderLayout(15, 15));
        panelGlowny.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        f.setContentPane(panelGlowny);

        Font czcionka = new Font("SansSerif", Font.PLAIN, 15);
        Color fiolet = new Color(140, 20, 180);

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JTextField tf1 = new JTextField("Poland", 12);
        JTextField tf2 = new JTextField("Warsaw", 12);
        JTextField tf3 = new JTextField("USD", 6);

        tf1.setFont(czcionka);
        tf2.setFont(czcionka);
        tf3.setFont(czcionka);

        JButton b = new JButton("szukaj info");
        b.setFont(czcionka);
        b.setBackground(new Color(225, 235, 245));

        p1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(fiolet), " wyszukiwarka "),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        p1.add(new JLabel("podaj kraj:"));
        p1.add(tf1);
        p1.add(new JLabel("podaj miasto:"));
        p1.add(tf2);
        p1.add(new JLabel("waluta docelowa:"));
        p1.add(tf3);
        p1.add(b);

        panelGlowny.add(p1, BorderLayout.SOUTH);

        JPanel p2 = new JPanel(new GridLayout(2, 1, 0, 15));

        JTextArea ta1 = new JTextArea();
        if (w == null) {
            ta1.setText("nie znaleziono pogody");
        } else {
            ta1.setText(w);
        }
        ta1.setEditable(false);
        ta1.setLineWrap(true);
        ta1.setWrapStyleWord(true);
        ta1.setFont(czcionka);
        ta1.setBackground(new Color(250, 250, 250));
        ta1.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(ta1);
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(fiolet), " info o pogodzie "),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JTextArea ta2 = new JTextArea();
        ta2.setEditable(false);
        ta2.setFont(czcionka);
        ta2.setBackground(new Color(250, 250, 250));
        ta2.setMargin(new Insets(10, 10, 10, 10));
        ta2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(fiolet), " przeliczniki "),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        String txt = "- kurs USD wobec waluty z Poland: \n > ";
        if (r1 == null) {
            txt = txt + "brak info";
        } else {
            txt = txt + r1;
        }
        txt = txt + "\n\n- kurs PLN wobec waluty z Poland (wg NBP): \n > ";
        if (r2 == null) {
            txt = txt + "brak info";
        } else {
            txt = txt + r2;
        }
        ta2.setText(txt);

        p2.add(ta2);
        p2.add(sp);
        p2.setPreferredSize(new Dimension(380, 0));

        panelGlowny.add(p2, BorderLayout.EAST);

        JFXPanel fxp = new JFXPanel();

        JPanel ramkaWiki = new JPanel(new BorderLayout());
        ramkaWiki.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(fiolet), " podglad z wikipedii "),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        ramkaWiki.add(fxp, BorderLayout.CENTER);

        panelGlowny.add(ramkaWiki, BorderLayout.CENTER);

        Platform.runLater(() -> {
            WebView web = new WebView();
            fxp.setScene(new Scene(web));
            web.getEngine().load("https://en.wikipedia.org/wiki/Warsaw");
        });

        b.addActionListener(e -> {
            String k = tf1.getText().trim();
            String m = tf2.getText().trim();
            String wa = tf3.getText().trim();

            new Thread(() -> {
                Service s2 = new Service(k);
                String w2 = s2.getWeather(m);
                Double rr1 = s2.getRateFor(wa);
                Double rr2 = s2.getNBPRate();

                SwingUtilities.invokeLater(() -> {
                    if (w2 == null) {
                        ta1.setText("nie znaleziono pogody");
                    } else {
                        ta1.setText(w2);
                    }

                    String t = "- kurs " + wa + " wobec waluty z " + k + ": \n > ";
                    if (rr1 == null) {
                        t = t + "brak info";
                    } else {
                        t = t + rr1;
                    }

                    t = t + "\n\n- kurs PLN wobec waluty z " + k + " (wg NBP): \n > ";
                    if (rr2 == null) {
                        t = t + "brak info";
                    } else {
                        t = t + rr2;
                    }

                    ta2.setText(t);
                });

                Platform.runLater(() -> {
                    Scene sc = fxp.getScene();
                    if(sc != null && sc.getRoot() instanceof WebView) {
                        WebView wv = (WebView) sc.getRoot();
                        wv.getEngine().load("https://en.wikipedia.org/wiki/" + m.replace(" ", "_"));
                    }
                });
            }).start();
        });

        f.setVisible(true);
    }
}
