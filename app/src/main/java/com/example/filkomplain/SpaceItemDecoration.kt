import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val spaceInDp: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val spaceInPixels = convertDpToPixels(view.context, spaceInDp)

        //outRect.top = spaceInPixels
        outRect.bottom = spaceInPixels
        //outRect.left = spaceInPixels
        //outRect.right = spaceInPixels
    }

    private fun convertDpToPixels(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }
}
