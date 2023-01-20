import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.persistence.NamedQuery;

public class SpELDemo {
    public static void main(String[] args) {

        Car car = new Car("test-test-car");

        ExpressionParser parser = new SpelExpressionParser();
        ParserContext ctx = new TemplateParserContext();

        Expression exp = parser.parseExpression("This is expression: #{#car.name} #{#car.name.length()}", ctx);
        EvaluationContext eCtx = new StandardEvaluationContext();
        eCtx.setVariable("car", car);
        Object result = exp.getValue(eCtx);
        System.out.println(result);

        //:text
        //:#{#text}

        String value = "SELECT FROM :134test123";
        String v = value.replaceAll(":[a-z,A-Z,0-9]+", "value");
        System.out.println(v);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Car {
        private String name;
    }
}
