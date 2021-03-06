package ru.mail.teamcity.web.parameters;

import jetbrains.buildServer.serverSide.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.teamcity.web.parameters.data.Option;
import ru.mail.teamcity.web.parameters.provider.WebParameterProvider;
import ru.mail.teamcity.web.parameters.data.Options;
import ru.mail.teamcity.web.parameters.manager.WebOptionsManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: g.chernyshev
 * Date: 05/03/16
 * Time: 14:32
 */
public class DynamicWebBuildStartContextProcessor implements BuildStartContextProcessor {

    @NotNull
    private final WebOptionsManager webOptionsManager;

    public DynamicWebBuildStartContextProcessor(@NotNull WebOptionsManager webOptionsManager) {
        this.webOptionsManager = webOptionsManager;
    }

    @Override
    public void updateParameters(@NotNull BuildStartContext context) {
        SBuildType buildType = context.getBuild().getBuildType();
        if (null == buildType) {
            return;
        }

        Collection<Parameter> buildParameters = buildType.getParametersCollection();
        for (Parameter parameter : buildParameters) {
            ControlDescription description = parameter.getControlDescription();
            // check if parameter is of web param provider type
            if (null != description && description.getParameterType().equals(WebParameterProvider.PARAMETER_TYPE)) {
                String buildValue = context.getBuild().getBuildOwnParameters().get(parameter.getName());
                // check if value from build is not provided and we don't have any default value
                if (buildValue.isEmpty() && parameter.getValue().isEmpty()) {
                    String url = description.getParameterTypeArguments().get(WebParameterProvider.URL_PARAMETER);
                    String format = description.getParameterTypeArguments().get(WebParameterProvider.FORMAT_PARAMETER);
                    Map<String, String> errors = new HashMap<>();

                    Options options = webOptionsManager.read(url, format, errors);
                    for (Option option : options.getOptions()) {
                        if (option.isEnabled() && option.isDefault()) {
                            context.addSharedParameter(parameter.getName(), option.getValue());
                            break;
                        }
                    }
                }
            }
        }
    }
}