from functions import TelegramInterface, notification_message_builder
from utilities.common import iterate_over_iterable

t = TelegramInterface()


def write_to_txt():
    with open("results.txt", "w") as f:
        f.write("\n".join(map(notification_message_builder,
                t.filter_by_type_worker("exam"))))


write_to_txt()
