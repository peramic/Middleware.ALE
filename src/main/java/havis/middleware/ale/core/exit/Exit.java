package havis.middleware.ale.core.exit;

import havis.middleware.ale.core.exit.event.EventHandler;
import havis.middleware.ale.exit.event.AddStringsArgs;
import havis.middleware.ale.exit.event.AddStringsFailedArgs;
import havis.middleware.ale.exit.event.BeginArgs;
import havis.middleware.ale.exit.event.DefineAssocTableSpecArgs;
import havis.middleware.ale.exit.event.DefineAssocTableSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineCCSpecArgs;
import havis.middleware.ale.exit.event.DefineCCSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineECSpecArgs;
import havis.middleware.ale.exit.event.DefineECSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineEPCCacheSpecArgs;
import havis.middleware.ale.exit.event.DefineEPCCacheSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineLRSpecArgs;
import havis.middleware.ale.exit.event.DefineLRSpecFailedArgs;
import havis.middleware.ale.exit.event.DefinePCSpecArgs;
import havis.middleware.ale.exit.event.DefinePCSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineRNGSpecArgs;
import havis.middleware.ale.exit.event.DefineRNGSpecFailedArgs;
import havis.middleware.ale.exit.event.DefineTMSpecArgs;
import havis.middleware.ale.exit.event.DefineTMSpecFailedArgs;
import havis.middleware.ale.exit.event.DeliverArgs;
import havis.middleware.ale.exit.event.DepleteArgs;
import havis.middleware.ale.exit.event.DepleteFailedArgs;
import havis.middleware.ale.exit.event.ErrorArgs;
import havis.middleware.ale.exit.event.EvaluateArgs;
import havis.middleware.ale.exit.event.EventArgs;
import havis.middleware.ale.exit.event.ExecutePCOpSpecsArgs;
import havis.middleware.ale.exit.event.ExecutePCOpSpecsFailedArgs;
import havis.middleware.ale.exit.event.ExecuteTagArgs;
import havis.middleware.ale.exit.event.FilterArgs;
import havis.middleware.ale.exit.event.FilterTagArgs;
import havis.middleware.ale.exit.event.GetArgs;
import havis.middleware.ale.exit.event.GetFailedArgs;
import havis.middleware.ale.exit.event.GetStringArgs;
import havis.middleware.ale.exit.event.GetStringFailedArgs;
import havis.middleware.ale.exit.event.GetStringsArgs;
import havis.middleware.ale.exit.event.GetStringsFailedArgs;
import havis.middleware.ale.exit.event.ImmediateCCSpecArgs;
import havis.middleware.ale.exit.event.ImmediateCCSpecFailedArgs;
import havis.middleware.ale.exit.event.ImmediateECSpecArgs;
import havis.middleware.ale.exit.event.ImmediateECSpecFailedArgs;
import havis.middleware.ale.exit.event.ImmediatePCSpecArgs;
import havis.middleware.ale.exit.event.ImmediatePCSpecFailedArgs;
import havis.middleware.ale.exit.event.InformationArgs;
import havis.middleware.ale.exit.event.NotifyArgs;
import havis.middleware.ale.exit.event.NotifyMessageArgs;
import havis.middleware.ale.exit.event.NotifyPortArgs;
import havis.middleware.ale.exit.event.NotifyTagArgs;
import havis.middleware.ale.exit.event.PollArgs;
import havis.middleware.ale.exit.event.PollCCSpecArgs;
import havis.middleware.ale.exit.event.PollCCSpecFailedArgs;
import havis.middleware.ale.exit.event.PollFailedArgs;
import havis.middleware.ale.exit.event.PutArgs;
import havis.middleware.ale.exit.event.PutAssocTableEntriesFailedArgs;
import havis.middleware.ale.exit.event.RemoveStringArgs;
import havis.middleware.ale.exit.event.RemoveStringFailedArgs;
import havis.middleware.ale.exit.event.RemoveStringsArgs;
import havis.middleware.ale.exit.event.RemoveStringsFailedArgs;
import havis.middleware.ale.exit.event.ReplenishArgs;
import havis.middleware.ale.exit.event.ReplenishFailedArgs;
import havis.middleware.ale.exit.event.ReportArgs;
import havis.middleware.ale.exit.event.SetLRPropertiesArgs;
import havis.middleware.ale.exit.event.SetLRPropertiesFailedArgs;
import havis.middleware.ale.exit.event.SetStringsArgs;
import havis.middleware.ale.exit.event.SetStringsFailedArgs;
import havis.middleware.ale.exit.event.StateArgs;
import havis.middleware.ale.exit.event.SubscribeArgs;
import havis.middleware.ale.exit.event.SubscribeFailedArgs;
import havis.middleware.ale.exit.event.TriggerArgs;
import havis.middleware.ale.exit.event.UndefineArgs;
import havis.middleware.ale.exit.event.UndefineFailedArgs;
import havis.middleware.ale.exit.event.UnsubscribeArgs;
import havis.middleware.ale.exit.event.UnsubscribeFailedArgs;
import havis.middleware.ale.exit.event.UpdateLRSpecArgs;
import havis.middleware.ale.exit.event.UpdateLRSpecFailedArgs;
import havis.middleware.ale.exit.event.WarningArgs;

public class Exit {

	public static class Cycle {

		public static EventHandler<StateArgs> State;

		public static EventHandler<TriggerArgs> Start;

		public static EventHandler<TriggerArgs> Stop;

		public static EventHandler<BeginArgs> Begin;

		public static EventHandler<EvaluateArgs> Evaluate;

		public static EventHandler<NotifyArgs> Notify;

		public static EventHandler<FilterArgs> Filter;

		public static EventHandler<ReportArgs> Report;

		public static EventHandler<ErrorArgs> Error;
	}

	/**
	 * Static instance
	 */
	static Exit instance = new Exit();

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static Exit getInstance() {
		return instance;
	}

	public static class Common {

		public static class _ extends Common {
		}

		public static EventHandler<ErrorArgs> Error;

		public static EventHandler<WarningArgs> Warning;

		public static EventHandler<InformationArgs> Information;
	}

	public static class _ extends Common {
	}

	public static class Service {

		public static class LR {

			public static class _ extends LR {
			}

			public static EventHandler<DefineLRSpecArgs> Define;

			public static EventHandler<DefineLRSpecFailedArgs> DefineFailed;

			public static EventHandler<UpdateLRSpecArgs> Update;

			public static EventHandler<UpdateLRSpecFailedArgs> UpdateFailed;

			public static EventHandler<UndefineArgs> Undefine;

			public static EventHandler<UndefineFailedArgs> UndefineFailed;

			public static EventHandler<GetArgs> Get;

			public static EventHandler<GetFailedArgs> GetFailed;

			public static EventHandler<EventArgs> GetNames;

			public static EventHandler<AddStringsArgs> AddReaders;

			public static EventHandler<AddStringsFailedArgs> AddReadersFailed;

			public static EventHandler<SetStringsArgs> SetReaders;

			public static EventHandler<SetStringsFailedArgs> SetReadersFailed;

			public static EventHandler<RemoveStringsArgs> RemoveReaders;

			public static EventHandler<RemoveStringsFailedArgs> RemoveReadersFailed;

			public static EventHandler<SetLRPropertiesArgs> SetProperties;

			public static EventHandler<SetLRPropertiesFailedArgs> SetPropertiesFailed;

			public static EventHandler<GetStringArgs> GetPropertyValue;

			public static EventHandler<GetStringFailedArgs> GetPropertyValueFailed;
		}

		public static class TM {

			public static class _ extends TM {
			}

			public static EventHandler<DefineTMSpecArgs> Define;

			public static EventHandler<DefineTMSpecFailedArgs> DefineFailed;

			public static EventHandler<UndefineArgs> Undefine;

			public static EventHandler<UndefineFailedArgs> UndefineFailed;

			public static EventHandler<GetArgs> Get;

			public static EventHandler<GetFailedArgs> GetFailed;

			public static EventHandler<GetArgs> GetNames;
		}

		public static class EC {

			public static class _ extends EC {
			}

			public static EventHandler<DefineECSpecArgs> Define;

			public static EventHandler<DefineECSpecFailedArgs> DefineFailed;

			public static EventHandler<UndefineArgs> Undefine;

			public static EventHandler<UndefineFailedArgs> UndefineFailed;

			public static EventHandler<GetArgs> Get;

			public static EventHandler<GetFailedArgs> GetFailed;

			public static EventHandler<EventArgs> GetNames;

			public static EventHandler<SubscribeArgs> Subscribe;

			public static EventHandler<SubscribeFailedArgs> SubscribeFailed;

			public static EventHandler<UnsubscribeArgs> Unsubscribe;

			public static EventHandler<UnsubscribeFailedArgs> UnsubscribeFailed;

			public static EventHandler<PollArgs> Poll;

			public static EventHandler<PollFailedArgs> PollFailed;

			public static EventHandler<ImmediateECSpecArgs> Immediate;

			public static EventHandler<ImmediateECSpecFailedArgs> ImmediateFailed;

			public static EventHandler<GetArgs> GetSubscribers;

			public static EventHandler<GetFailedArgs> GetSubscribersFailed;
		}

		public static class CC {

			public static class _ extends CC {
			}

			public static EventHandler<DefineCCSpecArgs> Define;

			public static EventHandler<DefineCCSpecFailedArgs> DefineFailed;

			public static EventHandler<UndefineArgs> Undefine;

			public static EventHandler<UndefineFailedArgs> UndefineFailed;

			public static EventHandler<GetArgs> Get;

			public static EventHandler<GetFailedArgs> GetFailed;

			public static EventHandler<EventArgs> GetNames;

			public static EventHandler<SubscribeArgs> Subscribe;

			public static EventHandler<SubscribeFailedArgs> SubscribeFailed;

			public static EventHandler<UnsubscribeArgs> Unsubscribe;

			public static EventHandler<UnsubscribeFailedArgs> UnsubscribeFailed;

			public static EventHandler<PollCCSpecArgs> Poll;

			public static EventHandler<PollCCSpecFailedArgs> PollFailed;

			public static EventHandler<ImmediateCCSpecArgs> Immediate;

			public static EventHandler<ImmediateCCSpecFailedArgs> ImmediateFailed;

			public static EventHandler<GetArgs> GetSubscribers;

			public static EventHandler<GetFailedArgs> GetSubscribersFailed;

			public static class Cache {

				public static class _ extends Cache {
				}

				public static EventHandler<DefineEPCCacheSpecArgs> Define;

				public static EventHandler<DefineEPCCacheSpecFailedArgs> DefineFailed;

				public static EventHandler<UndefineArgs> Undefine;

				public static EventHandler<UndefineFailedArgs> UndefineFailed;

				public static EventHandler<GetArgs> Get;

				public static EventHandler<GetFailedArgs> GetFailed;

				public static EventHandler<EventArgs> GetNames;

				public static EventHandler<ReplenishArgs> Replenish;

				public static EventHandler<ReplenishFailedArgs> ReplenishFailed;

				public static EventHandler<DepleteArgs> Deplete;

				public static EventHandler<DepleteFailedArgs> DepleteFailed;

				public static EventHandler<GetArgs> GetContents;

				public static EventHandler<GetFailedArgs> GetContentsFailed;
			}

			public static class Association {

				public static class _ extends Association {
				}

				public static EventHandler<DefineAssocTableSpecArgs> Define;

				public static EventHandler<DefineAssocTableSpecFailedArgs> DefineFailed;

				public static EventHandler<UndefineArgs> Undefine;

				public static EventHandler<UndefineFailedArgs> UndefineFailed;

				public static EventHandler<GetArgs> Get;

				public static EventHandler<GetFailedArgs> GetFailed;

				public static EventHandler<EventArgs> GetNames;

				public static EventHandler<PutArgs> PutEntries;

				public static EventHandler<PutAssocTableEntriesFailedArgs> PutEntriesFailed;

				public static EventHandler<GetStringArgs> GetValue;

				public static EventHandler<GetStringFailedArgs> GetValueFailed;

				public static EventHandler<GetStringsArgs> GetEntries;

				public static EventHandler<GetStringsFailedArgs> GetEntriesFailed;

				public static EventHandler<RemoveStringArgs> RemoveEntry;

				public static EventHandler<RemoveStringFailedArgs> RemoveEntryFailed;

				public static EventHandler<RemoveStringsArgs> RemoveEntries;

				public static EventHandler<RemoveStringsFailedArgs> RemoveEntriesFailed;
			}

			public static class Random {

				public static class _ extends Random {
				}

				public static EventHandler<DefineRNGSpecArgs> Define;

				public static EventHandler<DefineRNGSpecFailedArgs> DefineFailed;

				public static EventHandler<UndefineArgs> Undefine;

				public static EventHandler<UndefineFailedArgs> UndefineFailed;

				public static EventHandler<GetArgs> Get;

				public static EventHandler<GetFailedArgs> GetFailed;

				public static EventHandler<EventArgs> GetNames;
			}
		}

		public static class PC {

			public static class _ extends PC {
			}

			public static EventHandler<DefinePCSpecArgs> Define;

			public static EventHandler<DefinePCSpecFailedArgs> DefineFailed;

			public static EventHandler<UndefineArgs> Undefine;

			public static EventHandler<UndefineFailedArgs> UndefineFailed;

			public static EventHandler<GetArgs> Get;

			public static EventHandler<GetFailedArgs> GetFailed;

			public static EventHandler<GetArgs> GetNames;

			public static EventHandler<SubscribeArgs> Subscribe;

			public static EventHandler<SubscribeFailedArgs> SubscribeFailed;

			public static EventHandler<UnsubscribeArgs> Unsubscribe;

			public static EventHandler<UnsubscribeFailedArgs> UnsubscribeFailed;

			public static EventHandler<PollArgs> Poll;

			public static EventHandler<PollFailedArgs> PollFailed;

			public static EventHandler<ImmediatePCSpecArgs> Immediate;

			public static EventHandler<ImmediatePCSpecFailedArgs> ImmediateFailed;

			public static EventHandler<GetArgs> GetSubscribers;

			public static EventHandler<GetFailedArgs> GetSubscribersFailed;

			public static EventHandler<ExecutePCOpSpecsArgs> Execute;

			public static EventHandler<ExecutePCOpSpecsFailedArgs> ExecuteFailed;
		}
	}

	public static class Core {

		public static class Cycle {

			public static class CommonCycle {

				public static EventHandler<StateArgs> State;

				public static EventHandler<TriggerArgs> Start;

				public static EventHandler<TriggerArgs> Stop;

				public static EventHandler<BeginArgs> Begin;

				public static EventHandler<EvaluateArgs> Evaluate;

				public static EventHandler<NotifyArgs> Notify;

				public static EventHandler<ReportArgs> Report;

				public static EventHandler<DeliverArgs> Deliver;

				public static EventHandler<ErrorArgs> Error;
			}

			public static class TagCycle extends CommonCycle {

				public static EventHandler<FilterTagArgs> Filter;
			}

			public static class EventCycle extends TagCycle {

				public static class _ extends EventCycle {
				}
			}

			public static class CommandCycle extends TagCycle {

				public static class _ extends CommandCycle {
				}

				public static EventHandler<ExecuteTagArgs> Execute;

				public static EventHandler<ExecuteTagArgs> Executed;
			}

			public static class PortCycle extends TagCycle {

				public static class _ extends PortCycle {
				}

				public static EventHandler<ExecutePCOpSpecsArgs> Execute;
			}
		}
	}

	public static class Subscriber {

		public static class Controller {

			public static class _ extends Controller {
			}

			public static EventHandler<WarningArgs> Exited;

			public static EventHandler<WarningArgs> Faulted;

			public static EventHandler<ErrorArgs> ReinitFailed;
		}

		public static class Container {

			public static class _ extends Container {
			}

			public static EventHandler<WarningArgs> DeliverFailed;

			public static EventHandler<ErrorArgs> DeliverAborted;

			public static EventHandler<WarningArgs> ThresholdExceeded;

			public static EventHandler<InformationArgs> ThresholdEased;

			public static EventHandler<ErrorArgs> ReportsLost;

			public static EventHandler<ErrorArgs> InvokeFailed;
		}
	}

	public static class Reader {

		public static class Controller {

			public static class _ extends Controller {
			}

			public static EventHandler<ExecuteTagArgs> ExecutedTag;

			public static EventHandler<WarningArgs> Exited;

			public static EventHandler<WarningArgs> Faulted;

			public static EventHandler<ErrorArgs> ReinitFailed;

			public static EventHandler<InformationArgs> ConnectFailed;

			public static EventHandler<InformationArgs> ConnectionLost;

			public static EventHandler<InformationArgs> Reconnect;

			public static EventHandler<WarningArgs> ReconnectFailed;

			public static EventHandler<ErrorArgs> ReconnectAborted;

			public static EventHandler<ErrorArgs> Error;

			public static EventHandler<WarningArgs> Warning;

			public static EventHandler<InformationArgs> Information;

			public static class Callback {

				public static class _ extends Callback {
				}

				public static EventHandler<NotifyTagArgs> NotifyTag;

				public static EventHandler<NotifyPortArgs> NotifyPort;

				public static EventHandler<NotifyMessageArgs> NotifyMessage;
			}
		}
	}

	/**
	 * Returns the corresponding exit exception
	 * 
	 * @param e
	 *            The common exception
	 * @return The corresponding exit exception
	 */
	public static havis.middleware.ale.exit.event.ALEException exception(
			final Exception e) {
		return new havis.middleware.ale.exit.event.ALEException() {
			{
				setReason(Exit.toString(e));
			}
		};
	}

	public static String toString(Exception e) {
		return e.getClass().getName() + "\r\n" + e.getMessage() + "\r\n"
				+ e.getStackTrace();
	}
}