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
        JFrame f = new JFrame("apka");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1100, 700);
        f.setLayout(new BorderLayout());

        JPanel p1 = new JPanel(new FlowLayout());
        JTextField tf1 = new JTextField("Poland", 10);
        JTextField tf2 = new JTextField("Warsaw", 10);
        JTextField tf3 = new JTextField("USD", 5);
        JButton b = new JButton("szukaj");

        p1.add(new JLabel("kraj:"));
        p1.add(tf1);
        p1.add(new JLabel("miasto:"));
        p1.add(tf2);
        p1.add(new JLabel("waluta:"));
        p1.add(tf3);
        p1.add(b);

        f.add(p1, BorderLayout.NORTH);

        JPanel p2 = new JPanel(new GridLayout(2, 1));

        JTextArea ta1 = new JTextArea();
        if (w == null) {
            ta1.setText("brak");
        } else {
            ta1.setText(w);
        }
        ta1.setEditable(false);
        ta1.setLineWrap(true);
        ta1.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(ta1);
        sp.setBorder(BorderFactory.createTitledBorder("pogoda"));

        JTextArea ta2 = new JTextArea();
        ta2.setEditable(false);
        ta2.setFont(new Font("Arial", Font.BOLD, 14));
        ta2.setBorder(BorderFactory.createTitledBorder("kursy"));

        String txt = "1. kurs USD do waluty kraju (Poland): \n - ";
        if (r1 == null) {
            txt = txt + "brak";
        } else {
            txt = txt + r1;
        }
        txt = txt + "\n\n2. kurs PLN do waluty kraju (Poland) NBP: \n - ";
        if (r2 == null) {
            txt = txt + "brak";
        } else {
            txt = txt + r2;
        }
        ta2.setText(txt);

        p2.add(sp);
        p2.add(ta2);
        p2.setPreferredSize(new Dimension(350, 0));

        f.add(p2, BorderLayout.WEST);

        JFXPanel fxp = new JFXPanel();
        f.add(fxp, BorderLayout.CENTER);

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
                        ta1.setText("brak danych");
                    } else {
                        ta1.setText(w2);
                    }

                    String t = "1. kurs " + wa + " do waluty kraju (" + k + "): \n -> ";
                    if (rr1 == null) {
                        t = t + "brak";
                    } else {
                        t = t + rr1;
                    }

                    t = t + "\n\n2. kurs PLN do waluty kraju (" + k + ") NBP: \n -> ";
                    if (rr2 == null) {
                        t = t + "brak";
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
