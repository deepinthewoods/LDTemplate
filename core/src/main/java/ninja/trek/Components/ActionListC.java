package ninja.trek.Components;

import ninja.trek.Main;
import ninja.trek.actionlist.ActionList;

public class ActionListC extends Component{
    public ActionList actions = new ActionList();


    @Override
    public void update(float dt, Main main) {
        actions.update(dt);
    }

    @Override
    public void updateRender(float dt, Main main) {
        actions.updateRender(dt);
    }

    @Override
    public void onAdded(Main main) {
        actions.inserted(e);
    }

    @Override
    public void onRemove(Main main) {
        actions.clearWithDelayed();
    }

    // Convenience: allow game code to schedule actions
    public void addToStart(ninja.trek.actionlist.Action a){ actions.addToStart(a); a.parent = actions; }
    public void addToEnd(ninja.trek.actionlist.Action a){ actions.addToEnd(a); a.parent = actions; }
}
