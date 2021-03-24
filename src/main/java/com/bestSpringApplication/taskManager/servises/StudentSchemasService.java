package com.bestSpringApplication.taskManager.servises;

import com.bestSpringApplication.taskManager.utils.exceptions.forClient.ContentNotFoundException;
import com.bestSpringApplication.taskManager.utils.exceptions.forClient.TaskClosedException;
import com.bestSpringApplication.taskManager.utils.exceptions.forClient.UserNotFoundException;
import com.bestSpringApplication.taskManager.models.classes.DependencyWithRelationType;
import com.bestSpringApplication.taskManager.models.enums.Role;
import com.bestSpringApplication.taskManager.models.abstracts.AbstractStudySchema;
import com.bestSpringApplication.taskManager.models.abstracts.AbstractTask;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentSchemasService {

    @NonNull private final MasterSchemasService masterSchemasService;
    @NonNull private final UserService userService;
    @NonNull private final UserTaskRelationService utrService;

    //       student_id -> [ schema_key -> schema,...]
    private Map<String,Map<String, AbstractStudySchema>> studentsWithSchemas;

    @PostConstruct
    private void init(){
        studentsWithSchemas = new HashMap<>();
    }

    public void setSchemaToStudent(String studentId,String schemaKey){
        userService.validateExistsAndContainsRole(studentId,Role.STUDENT);
        Optional
                .ofNullable(studentsWithSchemas.get(studentId))
                .orElseGet(() -> studentsWithSchemas.put(studentId, new HashMap<>()));
        AbstractStudySchema masterSchema = masterSchemasService.schemaByKey(schemaKey);
        AbstractStudySchema clonedMasterSchema = SerializationUtils.clone(masterSchema);
        studentsWithSchemas.get(studentId).put(schemaKey,clonedMasterSchema);
        utrService.prepareFirstTasks(clonedMasterSchema,studentId);
    }

    public boolean canStartTask(String schemaKey, String studentId, String taskId){
        if (!utrService.existsBySchemaIdAndUserIdAndTaskId(schemaKey, taskId, studentId)) {
            boolean parentsCompleted = true;
            AbstractStudySchema schema = getStudentSchemaOrThrow(studentId, schemaKey);
            List<DependencyWithRelationType> dependencies = schema.getDependencies();
            AbstractTask task = specificTaskOfStudentSchema(schemaKey, studentId, taskId);
        }
        return false;
    }
    public void forceStartTask(String schemaKey, String studentId, String taskId){
        startTask(schemaKey, studentId, taskId);
    }

    public void startTaskWithValidation(String schemaKey, String studentId, String taskId){
        if (canStartTask(schemaKey, studentId, taskId)){
            startTask(schemaKey, studentId, taskId);
        }else {
            throw new TaskClosedException("Задание невозможно начать (REWRITE EX TEXT)");
        }
    }

    public AbstractTask specificTaskOfStudentSchema(String schemaKey, String studentId, String taskId){
        AbstractStudySchema schema = getStudentSchemaOrThrow(studentId, schemaKey);
        return Optional
                .ofNullable(schema.getTasksMap().get(taskId))
                .orElseThrow(() -> new ContentNotFoundException("Задание не найдено"));
    }

    public List<AbstractTask> allOpenedStudentTasks(String studentId){
        Collection<AbstractStudySchema> studentSchemas = getStudentSchemasOrThrow(studentId).values();
        List<AbstractTask> tasks = new ArrayList<>();
        studentSchemas.forEach(schema->
                schema.getTasksMap()
                        .values()
                        .stream()
                        .filter(AbstractTask::isOpened)
                        .forEach(tasks::add));
        return tasks;
    }

    public List<AbstractTask> openedStudentTasks(String studentId,String schemaKey){
        return Optional.ofNullable(studentsWithSchemas.get(studentId))
                .map(schemasMap->Optional.ofNullable(schemasMap.get(schemaKey))
                        .map(schema->
                                schema.getTasksMap()
                                        .values().stream()
                                        .filter(AbstractTask::isOpened)
                                        .collect(Collectors.toList()))
                        .orElseThrow(()->new ContentNotFoundException("Данный курс не назначен")))
                .orElseThrow(()->new UserNotFoundException("Студент не найден"));
    }

    public List<AbstractTask> studentSchemasRootTasks(String studentId){
        return getStudentSchemasOrThrow(studentId)
                .values().stream()
                .map(AbstractStudySchema::getRootTask)
                .collect(Collectors.toList());
    }

    public AbstractStudySchema getStudentSchemaOrThrow(String studentId,String schemaKey){
        return Optional
                .ofNullable(getStudentSchemasOrThrow(studentId).get(schemaKey))
                .orElseThrow(()->new ContentNotFoundException("Курс не назначен или не существует"));
    }

    public Map<String, AbstractStudySchema> getStudentSchemasOrThrow(String studentId) {
        userService.validateExistsAndContainsRole(studentId,Role.STUDENT);
        return Optional
                .ofNullable(studentsWithSchemas.get(studentId))
                .orElseThrow(() -> new UserNotFoundException("Курсы не назначены"));
    }

    private void startTask(String schemaKey, String studentId, String taskId) {
        AbstractStudySchema schema = getStudentSchemaOrThrow(studentId, schemaKey);
        AbstractTask task = specificTaskOfStudentSchema(schemaKey, studentId, taskId);
        utrService.prepareTask(schema,task,studentId);
    }
}




