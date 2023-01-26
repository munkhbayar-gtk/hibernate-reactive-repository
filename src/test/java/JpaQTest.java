import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JpaQTest {

    public static void main(String[] args) {
        String q_e_p= "$1__HASH__{";
        Pattern pattern = Pattern.compile("[:?](#\\{#user.[a-z,A-Z,0-9]+\\})");
        String q = "FROM User u WHERE u.name LIKE CONCAT('%',:#{#user.name},'%') :#{#user.id}";
        Matcher m = pattern.matcher(q);

        Pattern EXP_NM = Pattern.compile("#\\{#(.*)\\}");
        while(m.find()) {
            String exp = m.group(1);
            System.out.println(exp);

            Matcher mm = EXP_NM.matcher(exp);
            mm.find();
            System.out.println(mm.group(1).replace(".", "_"));
        }

        /*
        String r = pattern.matcher(q).replaceAll(q_e_p);
        System.out.println(r);


        String e_p = "$1#{";
        Pattern up = Pattern.compile("([:?])__HASH__\\{");

        String rr = up.matcher(r).replaceAll(e_p);
        System.out.println(rr);
         */
    }

}
