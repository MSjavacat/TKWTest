package com.ms.seckill.vo;

import com.ms.seckill.annotation.IsMobile;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author MS
 * @create 2022-08-31-9:28
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    boolean isRequired = false;

    @Override
    public void initialize(IsMobile constraintAnnotation) {
        isRequired = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (isRequired) {
            return StringUtils.isEmpty(s);
        }else {
            if (StringUtils.isEmpty(s)) {
                return true;
            }else {
                return StringUtils.isEmpty(s);
            }
        }
    }
}
