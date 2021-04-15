package com.bestSpringApplication.taskManager.utils.parsers.xml;

import com.bestSpringApplication.taskManager.models.abstracts.AbstractTask;
import com.bestSpringApplication.taskManager.models.classes.DependencyWithRelationType;
import com.bestSpringApplication.taskManager.models.classes.DefaultTask;
import com.bestSpringApplication.taskManager.models.enums.RelationType;
import com.bestSpringApplication.taskManager.utils.DateHandler;
import com.bestSpringApplication.taskManager.utils.StudyParseHandler;
import com.bestSpringApplication.taskManager.utils.exceptions.internal.TaskParseException;
import com.bestSpringApplication.taskManager.utils.parsers.TaskParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Element;

import java.util.*;

@Slf4j
public class XmlTaskParser implements TaskParser {

    private final Map<String, AbstractTask> taskMap;
    private final List<DependencyWithRelationType> dependencies;

    public XmlTaskParser(Element arg) {
        taskMap = new HashMap<>();
        dependencies = new ArrayList<>();
        parseAndMakeDependencies(arg);
    }

    @Override
    public Map<String, AbstractTask> getTasks(){
        return taskMap;
    }

    @Override
    public List<DependencyWithRelationType> getHierarchicalDependencies() {
        return dependencies;
    }

    private void parseAndMakeDependencies(Element element) throws TaskParseException{
        log.trace("Receiving element = {}",element);
        Stack<Element> tasksStack = new Stack<>();
        List<AbstractTask> taskList = new ArrayList<>();
        tasksStack.push(element);
        while (!tasksStack.empty()) {
            Element taskElemFromStack = tasksStack.pop();
            DefaultTask.DefaultTaskBuilder taskBuilder = DefaultTask.builder();
            String taskName = Optional.ofNullable(taskElemFromStack.getChildText("task-name"))
                    .orElseThrow(()->new TaskParseException("task id is empty!"));
            String taskId = Optional.ofNullable(taskElemFromStack.getChildText("task-id"))
                    .orElseThrow(()->new TaskParseException("task name is empty!"));
            Optional<Element> fieldListElem = Optional.ofNullable(taskElemFromStack.getChild("field-list"));
            Optional<Element> taskListElem = Optional.ofNullable(taskElemFromStack.getChild("task-list"));
            Optional<Element> taskNotesElem = Optional.ofNullable(taskElemFromStack.getChild("task-notes"));
            Optional<String> startDate = Optional.ofNullable(taskElemFromStack.getChildText("task-start-date"));
            Optional<String> endDate = Optional.ofNullable(taskElemFromStack.getChildText("task-end-date"));
            fieldListElem.ifPresent(fieldList ->
                    taskBuilder.fields(StudyParseHandler.xmlFieldToMap(fieldList, "field","field-no", "field-value"))
            );
            taskNotesElem.ifPresent(notes ->
                    taskBuilder.notes(StringUtils.normalizeSpace(StringEscapeUtils.unescapeHtml4(notes.getValue())))
            );
            taskListElem.ifPresent(tasksOpt->{
                taskBuilder.theme(true);
                Optional<List<Element>> tasks = Optional.ofNullable(tasksOpt.getChildren("task"));
                tasks.ifPresent(tasksListOpt->
                        tasksListOpt.forEach(el->{
                            String taskListElemId = el.getChildText("task-id");
                            dependencies.add(new DependencyWithRelationType(RelationType.HIERARCHICAL,taskId,taskListElemId));
                            tasksStack.push(el);
                        })
                );
            });
            String optimizedName = StringUtils.normalizeSpace(taskName).replaceAll(" ", "_");
            String defaultTime = "01-01-1970, 00:00:00";
            long formattedStartDate = DateHandler
                    .parseDateToLongFromFormat(startDate.orElse(defaultTime), "dd-MM-yyyy, HH:mm:ss");
            long formattedEndDate = DateHandler
                    .parseDateToLongFromFormat(endDate.orElse(defaultTime), "dd-MM-yyyy, HH:mm:ss");
            taskBuilder
                    .name(optimizedName)
                    .id(taskId)
                    .startDate(formattedStartDate)
                    .endDate(formattedEndDate);
            taskList.add(taskBuilder.build());
        }
        AbstractTask rootCourseTask = taskList.get(1);
        taskMap.put("root",rootCourseTask);
        taskList.forEach(task->taskMap.put(task.getId(),task));
    }

}
