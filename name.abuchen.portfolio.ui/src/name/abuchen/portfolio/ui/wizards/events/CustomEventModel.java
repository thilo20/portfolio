package name.abuchen.portfolio.ui.wizards.events;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class CustomEventModel extends BindingHelper.Model
{
    private Security security;
    private LocalDate date = LocalDate.now();
    private SecurityEvent.Type type = SecurityEvent.Type.NOTE;
    private String message = ""; //$NON-NLS-1$

    private SecurityEvent source;

    public CustomEventModel(Client client, Security security)
    {
        super(client);

        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        firePropertyChange("data", this.date, this.date = date); //$NON-NLS-1$
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        firePropertyChange("message", this.message, this.message = message); //$NON-NLS-1$
    }

    public void setSource(Object entry)
    {
        this.source = (SecurityEvent) entry;

        this.date = source.getDate();
        this.message = source.getDetails();
        this.type = source.getType();
    }

    @Override
    public void applyChanges()
    {
        if (source == null)
        {
            SecurityEvent event = new SecurityEvent(date, type, message);
            security.addEvent(event);
        }
        else
        {
            // update source object
            source.setDate(date);
            source.setDetails(message);
            source.setType(type);
        }
    }
}
