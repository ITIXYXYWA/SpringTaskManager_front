package com.bestSpringApplication.taskManager.Controllers;

import com.bestSpringApplication.taskManager.models.abstracts.AbstractTask;
import com.bestSpringApplication.taskManager.models.entities.User;
import com.bestSpringApplication.taskManager.models.entities.UserTaskState;
import com.bestSpringApplication.taskManager.models.enums.Status;
import com.bestSpringApplication.taskManager.servises.interfaces.SchemasService;
import com.bestSpringApplication.taskManager.servises.interfaces.StudyService;
import com.bestSpringApplication.taskManager.servises.interfaces.StudyStateService;
import com.bestSpringApplication.taskManager.servises.interfaces.UserService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("/study")
@RestController
@Slf4j
@RequiredArgsConstructor
public class StudentController {

    private final String OPENED_SCHEMAS =       "/schemas/opened";
    private final String OPENED_TASKS =         "/tasks/opened";
    private final String SCHEMA_STATE =         "/schema/{schemaId}/state";
    private final String OPENED_SCHEMA_TASKS =  "/schema/{schemaId}/tasks/opened";
    private final String FINISH_TASK =          "/schema/{schemaId}/task/{taskId}/finish";
    private final String START_TASK =           "/schema/{schemaId}/task/{taskId}/start";

    @NonNull private final StudyService studyService;
    @NonNull private final StudyStateService studyStateService;
    @NonNull private final UserService userService;
    @NonNull private final SchemasService schemasService;


    @GetMapping(OPENED_TASKS)
    public List<AbstractTask> openedTasks(@AuthenticationPrincipal User user){
        log.trace("request for all opened user tasks");
        String userId = user.getStringId();
        userService.validateUserExistsOrThrow(userId);
        return studyService.getAllOpenedUserTasks(userId);
    }

    @GetMapping(SCHEMA_STATE)
    public List<UserTaskState> stateOfUserSchema(@AuthenticationPrincipal User user, @PathVariable String schemaId){
        String userId = user.getStringId();
        log.trace("request for state of schema '{}' by user '{}'",schemaId, userId);
        schemasService.validateSchemaExistsOrThrow(schemaId);
        userService.validateUserExistsOrThrow(userId);
        return studyStateService.getSchemaStateByUserId(userId,schemaId);
    }

    @GetMapping(START_TASK)
    @ResponseStatus(HttpStatus.OK)
    public void startTask(@AuthenticationPrincipal User user,@PathVariable String schemaId, @PathVariable String taskId){
        log.trace("user '{}' try start task '{}' in schema '{}'",user,taskId,schemaId);
        schemasService.validateSchemaExistsOrThrow(schemaId);
        schemasService.validateTaskInSchemaExistsOrThrow(schemaId,taskId);
        userService.validateUserExistsOrThrow(user.getStringId());
        studyService.startTaskWithValidation(schemaId, user.getStringId(),taskId);
    }

    @GetMapping(FINISH_TASK)
    @ResponseStatus(HttpStatus.OK)
    public void finishTask(@AuthenticationPrincipal User user,@PathVariable String schemaId, @PathVariable String taskId){
        log.trace("user '{}' try finish task '{}' in schema '{}'",user,taskId,schemaId);
        schemasService.validateSchemaExistsOrThrow(schemaId);
        schemasService.validateTaskInSchemaExistsOrThrow(schemaId,taskId);
        userService.validateUserExistsOrThrow(user.getStringId());
        studyStateService.setStatusForUserTask(schemaId,user.getStringId(),taskId,Status.UNCONFIRMED);
    }

    @GetMapping(OPENED_SCHEMAS)
    public List<AbstractTask> openedSchemas(@AuthenticationPrincipal User user){
        log.trace("request for all user '{}' schemas",user);
        userService.validateUserExistsOrThrow(user.getStringId());
        return studyService.getUserSchemasRootTasks(user.getStringId());
    }

    @GetMapping(OPENED_SCHEMA_TASKS)
    public List<AbstractTask> openedSchemasTasks(@PathVariable String schemaId, @AuthenticationPrincipal User user){
        log.trace("request for opened tasks for user '{}' in schema '{}'",user,schemaId);
        schemasService.validateSchemaExistsOrThrow(schemaId);
        userService.validateUserExistsOrThrow(user.getStringId());
        return studyService.getOpenedUserTasksOfSchema(user.getStringId(),schemaId);
    }

}

