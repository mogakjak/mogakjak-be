package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.timer.dto.request.PomodoroStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TodoTargetTimeContractTest {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final UUID categoryId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 10, 5);

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {60, 3600, 86400})
    void createAndUpdateAcceptUnsetOrValidTargets(Integer target) {
        assertTrue(validator.validate(new CreateTodoRequest(categoryId, "독서", date, target)).isEmpty());
        assertTrue(validator.validate(new UpdateTodoRequest(categoryId, "독서", date, target)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 59, 86401})
    void createAndUpdateRejectOutOfRangeTargets(Integer target) {
        assertEquals("targetTimeInSeconds", validator.validate(
                new CreateTodoRequest(categoryId, "독서", date, target)).iterator().next().getPropertyPath().toString());
        assertEquals("targetTimeInSeconds", validator.validate(
                new UpdateTodoRequest(categoryId, "독서", date, target)).iterator().next().getPropertyPath().toString());
    }

    @Test
    void omittedAndExplicitNullTargetsDeserializeAsUnset() throws Exception {
        String fields = "\"categoryId\":\"" + categoryId + "\",\"task\":\"독서\",\"date\":\"2026-10-05\"";
        for (String json : new String[]{"{" + fields + "}", "{" + fields + ",\"targetTimeInSeconds\":null}"}) {
            CreateTodoRequest create = mapper.readValue(json, CreateTodoRequest.class);
            UpdateTodoRequest update = mapper.readValue(json, UpdateTodoRequest.class);
            assertNull(create.getTargetTimeInSeconds());
            assertNull(update.getTargetTimeInSeconds());
            assertTrue(validator.validate(create).isEmpty());
            assertTrue(validator.validate(update).isEmpty());
        }
    }

    @Test
    void otherRequiredTodoFieldsRemainRequired() {
        assertEquals(3, validator.validate(new CreateTodoRequest(null, "", null, null)).size());
        assertEquals(3, validator.validate(new UpdateTodoRequest(null, "", null, null)).size());
    }

    @Test
    void allPersonalTimerModesStillRequireTodo() {
        assertFalse(validator.validate(TimerStartRequest.builder().targetSeconds(1800L)
                .participationType(ParticipationType.INDIVIDUAL).build()).isEmpty());
        assertFalse(validator.validate(new StopwatchStartRequest(null, ParticipationType.INDIVIDUAL,
                null, null, null)).isEmpty());
        assertFalse(validator.validate(new PomodoroStartRequest(null, 1200L, 300L, 2,
                ParticipationType.INDIVIDUAL, null, null, null)).isEmpty());
    }

    @Test
    void countdownAndPomodoroExecutionSettingsRemainRequired() {
        assertFalse(validator.validate(TimerStartRequest.builder().todoId(UUID.randomUUID())
                .participationType(ParticipationType.INDIVIDUAL).build()).isEmpty());
        assertEquals(3, validator.validate(new PomodoroStartRequest(UUID.randomUUID(), null, null, null,
                ParticipationType.INDIVIDUAL, null, null, null)).size());
    }

    @Test
    void responseSerializesUnsetTargetAndProgressAsExplicitNull() {
        Todo todo = todo(null, 600);
        JsonNode full = mapper.valueToTree(TodoResponse.from(todo));
        JsonNode simple = mapper.valueToTree(SimpleTodoResponse.from(todo));
        assertTrue(full.has("targetTimeInSeconds"));
        assertTrue(full.get("targetTimeInSeconds").isNull());
        assertTrue(full.has("progressRate"));
        assertTrue(full.get("progressRate").isNull());
        assertEquals(600, full.get("actualTimeInSeconds").asInt());
        assertTrue(simple.has("targetTimeInSeconds"));
        assertTrue(simple.get("targetTimeInSeconds").isNull());
    }

    @Test
    void existingProgressCalculationIsPreserved() {
        assertEquals(0, TodoResponse.from(todo(3600, 0)).getProgressRate());
        assertEquals(33, TodoResponse.from(todo(3600, 1200)).getProgressRate());
        assertEquals(100, TodoResponse.from(todo(3600, 7200)).getProgressRate());
    }

    private Todo todo(Integer target, Integer actual) {
        return Todo.builder().category(Category.builder().build()).task("독서").date(date)
                .targetTimeInSeconds(target).actualTimeInSeconds(actual).build();
    }
}
