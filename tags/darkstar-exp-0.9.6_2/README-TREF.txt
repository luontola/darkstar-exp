
    TRANSPARENT REFERENCES

TransparentReferences are a technique for hiding the use of ManagedReferences 
from the application code when using Darkstar. ManagedObjects which are not 
within a ManagedReference are replaced on serialization with a 
TransparentReference proxy which implements the same interfaces as the 
ManagedObject (excluding the ManagedObject interface). As a result, the 
application programmer will not need to manually create ManagedReferences for 
each ManagedObject, and the code will be much cleaner.

Earlier you had to write as follows (from HelloPersistence3.java example)

    private ManagedReference subTaskRef = null;

    public TrivialTimedTask getSubTask() {
        if (subTaskRef == null)
            return null;

        return subTaskRef.get(TrivialTimedTask.class);
    }

    public void setSubTask(TrivialTimedTask subTask) {
        if (subTask == null) {
            subTaskRef = null;
            return;
        }
        DataManager dataManager = AppContext.getDataManager();
        subTaskRef = dataManager.createReference(subTask);
    }

But by relying on TransparentReferences the above code can be written as

    private Task subTask = null;

    public Task getSubTask() {
        return subTask;
    }

    public void setSubTask(Task subTask) {
        this.subTask = subTask;
    }

The TransparentReferences were implemented by Esko Luontola (Jackal von ÖRF) in 
the hope that they would be integrated into Darkstar. Until then you can use 
these files to patch your Darkstar Server. Feedback and bug reports are welcomed 
at the Project Darkstar Community Forums in the thread 
http://www.projectdarkstar.com/index.php?option=com_smf&Itemid=44&topic=329.0


    REQUIREMENTS

There are some requirements which must be met for the TransparentReferences to 
work correctly:

1. All instance fields must refer to ManagedObjects through an interface instead 
of the implementation class. Otherwise it won't be possible to assign the 
TransparentReference proxy to the field. Failure to do so will result in an 
exception being thrown during deserialization.

2. All ManagedObjects must implement the equals(Object) and hashCode() methods 
as follows:

    public boolean equals(Object obj) {
        return ManagedIdentity.equals(this, obj);
    }

    public int hashCode() {
        return ManagedIdentity.hashCode(this);
    }

As a result, the ManagedObject and its proxies are considered equal and the 
hashCode will stay the same through the ManagedObject's lifecycle. Also, the 
ManagedObject itself will not be loaded from the data store when only its equals 
and hashCode methods are used (as many Collections do).


    MARK FOR UPDATE

Since in the application code it is very hard to tell when you have a plain 
ManagedObject instance and when you have a TransparentReference proxy to the 
ManagedObject, there is a convenience method for marking objects for update:

    TransparentReferenceUtil.markForUpdate(Object);

This will use ManagedReference.getForUpdate if the ManagedObject has not yet 
been loaded from the data store. Otherwise DataManager.markForUpdate is used.


    CONVERTING AN APPLICATION TO USE TRANSPARENT REFERENCES

If you have an existing Darkstar application, you may follow this sequence to 
refactor your application to be compatible with TransparentReferences:

1. Replace all calls to AppContext.getDataManager().markForUpdate() and 
ManagedReference.getForUpdate() with TransparentReferenceUtil.markForUpdate().

2. Implement the equals(Object) and hashCode() methods in all of your 
ManagedObject classes as described earlier.

3. Find all usages of ManagedReference. Replace them with using the 
ManagedObject directly. You should have no fields or variables whose type is 
ManagedReference. Calling AppContext.getDataManager().createReference() is not 
needed, so you may remove all calls to it.

4. Use the extract interface refactoring (in IntelliJ IDEA "Refactor | Extract 
Interface") on all of your ManagedObjects. Replace all usages of the class with 
the interface (in IntelliJ IDEA "Refactor | Use Interface Where Possible" or as 
a part of the Extract Interface refactoring). If your code is well structured, 
this step should happen almost automatically (or if you already use interfaces, 
there is no need to do changes).

5. Make sure that no instance fields have the concrete class of a ManagedObject 
as their type. Test your program to make sure that no exceptions happen during 
deserialization. If an exception happens, it should tell you the field which had 
a wrong type. Changing the type of the field to an interface or java.lang.Object 
should solve the issue.


    ENABLING AND DISABLING TRANSPARENT REFERENCES

In darkstar-exp.properties, add the following values to darkstar.exp.hooks in 
order to enable the TransparentReferences extension. Omit them to disable it.

  net.orfjackal.darkstar.exp.mods.TransparentReferencesHook1of2
  net.orfjackal.darkstar.exp.mods.TransparentReferencesHook2of2
