package beans.factory.Impl;

import beans.factory.IContextFactory;
import beans.factory.IInitializeable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

public class XmlContextFactory implements IContextFactory {
    private DefaultListableBeanFactory beanFactory;
    private String location;

    public XmlContextFactory(String location) {
        this.location = location;
    }

    @Override
    public BeanFactory getFactory() {
        beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        int beanDefinitionsCount = reader.loadBeanDefinitions(location);
        //System.out.println("XmlContextFactory已加载" + beanDefinitionsCount + "个Beans");
        return beanFactory;
    }

}
