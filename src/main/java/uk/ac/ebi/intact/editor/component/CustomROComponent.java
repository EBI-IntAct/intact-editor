package uk.ac.ebi.intact.editor.component;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by anjali on 08/09/17.
 */

@FacesComponent(value = "uk.ac.ebi.intact.editor.component.CustomROComponent")
public class CustomROComponent extends UIComponentBase
{
    @Override
    public String getFamily()
    {
        return "za.co.knowles";
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        Boolean readOnly = new Boolean((String)getAttributes().get("readOnly"));
        if (readOnly == null)
        {
            readOnly = false;
        }

        processViewTree(this, readOnly);
    }

    private void processViewTree(UIComponent component, boolean setTo)
    {
        for (UIComponent child : component.getChildren())
        {
            if (UIInput.class.isAssignableFrom(child.getClass()))
            {
                UIInput inputText = (UIInput) child;
                if (!callMethod(inputText, "setReadonly", setTo))
                {
                    // second attempt.
                    callMethod(inputText, "setReadOnly", setTo);
                }
            }

            processViewTree(child, setTo);
        }
    }

    private boolean callMethod(UIInput inputText, String name, boolean setTo)
    {
        try
        {
            Method method = inputText.getClass().getMethod(name, boolean.class);
            method.invoke(inputText, setTo);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            return false;
        }
        return true;
    }
}
