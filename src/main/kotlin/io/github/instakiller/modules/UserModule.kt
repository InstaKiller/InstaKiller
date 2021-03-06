package io.github.instakiller.modules

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.instagram4j.instagram4j.IGClient
import com.github.instagram4j.instagram4j.models.user.User
import io.github.instakiller.console.ConsoleHelper.readBoolean
import io.github.instakiller.console.ConsoleHelper.readMultipleStrings
import io.github.instakiller.helpers.Downloader
import io.github.instakiller.helpers.LoggerHelper.loading
import io.github.instakiller.helpers.LoggerHelper.progress
import io.github.instakiller.helpers.UserHelper
import io.github.instakiller.utils.Constants.askStyle
import io.github.instakiller.utils.Constants.downloadDir
import io.github.instakiller.utils.Constants.errorStyle
import io.github.instakiller.utils.Constants.ter
import io.github.instakiller.utils.ImageUtil.viewImage
import io.github.instakiller.utils.Menus.userMenu
import io.github.instakiller.utils.Utility.getName

class UserModule(private val igClient: IGClient) : BaseModule(userMenu) {

    private val downloader: Downloader by lazy(LazyThreadSafetyMode.NONE) { Downloader(igClient.httpClient) }

    override fun run(): Int {
        when (super.run()) {
            0 -> return 0
            1 -> showMenu()
            2 -> showUsersInfoByUsername()
            3 -> downloadUsersProfilePictures(readMultipleStrings("username"))
            else -> {
                ter.println(errorStyle("Invalid input, try again!"))
                run()
            }
        }

        run()
        return 0
    }

    private fun downloadUsersProfilePictures(usernames: List<String>) {
        usernames.forEach { username ->
            loading { loadingDone ->
                val (user, error) = UserHelper(igClient).getUserInfoByUsername(username)
                loadingDone()
                progress { progressDone ->
                    if (user != null && error == null) downloadSingleUserProfilePicture(user, username, progressDone)
                    else {
                        ter.println(errorStyle("Skipping, User with username $username not found! => ${error?.message}"))
                        progressDone()
                    }
                }
            }
        }
    }

    private fun downloadSingleUserProfilePicture(
        user: User,
        username: String,
        progressDone: () -> Unit
    ) {
        val imageUrl = user.hd_profile_pic_url_info.url
        val imageName = imageUrl.getName()
        val (imageFile, downloadError) = downloader.download(imageUrl, "images/$username/profile_pictures/$imageName")
        progressDone()
        if (imageFile != null && downloadError == null) {
            ter.println(style("Image saved successfully to $downloadDir/images/$username/$imageName"))
            if (readBoolean("Do you want to open the image?(y/n)")) viewImage(imageFile)
        } else ter.println(errorStyle("Skipping, Failed to download image for $username => ${downloadError?.message}"))
    }

    private fun showUsersInfoByUsername() {
        val usernames = readMultipleStrings("username")
        usernames.forEach { username ->
            progress {
                val (userInfo, error) = UserHelper(igClient).getUserInfoByUsername(username)
                it()
                printUserInfo(userInfo, error)
            }
        }
    }

    private fun printUserInfo(userInfo: User?, error: Throwable?) {
        if (userInfo != null && error == null) {
            showShortUserInfo(userInfo)
            checkIfMoreUserInfoNeeded(userInfo)
        } else ter.println(errorStyle("Failed to get user info! Error: ${error?.message}"))
    }

    private fun checkIfMoreUserInfoNeeded(userInfo: User) {
        ter.println(askStyle("Do you want to see full user info? (y/n)"))
        if (readBoolean()) showFullUserInfo(userInfo)
    }

    companion object {

        private val style = TextColors.green + TextStyles.bold

        fun showShortUserInfo(user: User) {
            ter.println(style("Username: ${user.username}"))
            ter.println(style("Full Name: ${user.full_name}"))
            ter.println(style("Bio: ${user.biography}"))
            ter.println(style("Media Count: ${user.media_count}"))
            ter.println(style("Follower Count: ${user.follower_count}"))
            ter.println(style("Following Count: ${user.following_count}"))
        }

        fun showFullUserInfo(user: User) {
            showShortUserInfo(user)

            ter.println(style("Profile Url: https://instagram.com/${user.username}"))
            ter.println(style("Profile Pic Url: ${user.profile_pic_url}"))
            ter.println(style("Private or Public: ${if (user.is_private) "Private" else "Public"}"))
            ter.println(style("Verified or not (Blue tick): ${if (user.is_verified) "Verified" else "Not Verified"}"))
            ter.println(style("Business or Regular: ${if (user.is_business) "Business" else "Regular"}"))
            ter.println(style("External Url: ${user.external_url}"))
        }
    }
}