package com.balsam.oasis.common.registry.example;

import java.sql.Types;
import java.util.Date;

import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.PlsqlParamDef;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlRegistryImpl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OracleHRPlsqlConfig {

        private final PlsqlRegistryImpl plsqlRegistry;

        @PostConstruct
        public void registerPlsqlBlocks() {

                // Register the add_job_history procedure with dummy outputs using named
                // parameters
                plsqlRegistry.register(PlsqlDefinitionBuilder.builder("addJobHistory")
                                .plsql("""
                                                DECLARE
                                                    v_test_param VARCHAR2(20) := :p_test_param;
                                                    v_days NUMBER;
                                                    v_emp_id NUMBER := :p_emp_id;
                                                    v_start_date DATE := :p_start_date;
                                                    v_end_date DATE := :p_end_date;
                                                    v_job_id VARCHAR2(10) := :p_job_id;
                                                    v_dept_id NUMBER := :p_department_id;

                                                BEGIN
                                                    -- Call the original procedure
                                                    add_job_history(v_emp_id, v_start_date, v_end_date, v_job_id, v_dept_id);

                                                    -- Calculate days difference
                                                    v_days := v_end_date - v_start_date;

                                                    -- Set output parameters
                                                    :p_duration_days := v_days;
                                                    :p_result_message := 'Success: Employee ' || v_emp_id || ' job history added for ' || v_days || ' days';
                                                    :p_my_name := v_test_param;
                                                END;
                                                """)
                                .preProcessor((ctx) -> {
                                        ctx.addParam("p_test_param", "ahmad al-saheb");
                                })
                                .parameter(PlsqlParamDef.in("p_emp_id", Integer.class)
                                                .required(true)
                                                .sqlType(Types.INTEGER)
                                                .build())
                                .parameter(PlsqlParamDef.in("p_start_date", Date.class)
                                                .required(true)
                                                .sqlType(Types.DATE)
                                                .build())
                                .parameter(PlsqlParamDef.in("p_end_date", Date.class)
                                                .required(true)
                                                .sqlType(Types.DATE)
                                                .build())
                                .parameter(PlsqlParamDef.in("p_job_id", String.class)
                                                .required(true)
                                                .sqlType(Types.VARCHAR)
                                                .build())
                                .parameter(PlsqlParamDef.in("p_test_param", String.class)
                                                .required(true)
                                                .sqlType(Types.VARCHAR)
                                                .build())
                                .parameter(PlsqlParamDef.in("p_department_id", Integer.class)
                                                .required(false)
                                                .sqlType(Types.INTEGER)
                                                .build())
                                .parameter(PlsqlParamDef.out("p_result_message", String.class)
                                                .sqlType(Types.VARCHAR)
                                                .build())
                                .parameter(PlsqlParamDef.out("p_duration_days", Integer.class)
                                                .sqlType(Types.INTEGER)
                                                .build())
                                .parameter(PlsqlParamDef.out("p_my_name", Integer.class)
                                                .sqlType(Types.VARCHAR)
                                                .build())
                                .postProcessor((result, ctx) -> {
                                        // Add/modify any data in the output map (it's already mutable)
                                        result.put("execution_timestamp", System.currentTimeMillis());
                                        result.put("employee_status", "ACTIVE");
                                        result.put("processed_by", "PL/SQL Engine v2.0");

                                        // Add calculated data based on existing results
                                        Object durationDays = result.get("p_duration_days");
                                        if (durationDays instanceof Number) {
                                                int days = ((Number) durationDays).intValue();
                                                result.put("duration_weeks", Math.round(days / 7.0));
                                                result.put("duration_category",
                                                                days > 30 ? "LONG_TERM"
                                                                                : days > 7 ? "MEDIUM_TERM"
                                                                                                : "SHORT_TERM");
                                        }

                                        // Add input parameter summary
                                        result.put("input_summary", String.format(
                                                        "Employee %s: %s to %s in %s",
                                                        ctx.getParam("p_emp_id"),
                                                        ctx.getParam("p_start_date"),
                                                        ctx.getParam("p_end_date"),
                                                        ctx.getParam("p_job_id")));
                                        result.put("name",
                                                        ctx.getParam("p_test_param") + " - " + result.get("p_my_name"));

                                        // Return the modified outputs map
                                        return result;
                                })
                                .build());
        }
}