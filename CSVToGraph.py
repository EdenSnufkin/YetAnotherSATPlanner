import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

df = pd.read_csv("CSV_ComparedYETHSP.csv",header=None).to_numpy()
df = df.T
N = df[0].size
ind = np.arange(N)
width = 0.27
names = np.array(df[0],dtype=str)
names = np.char.strip(names,'problem_test_2/')

fig, axis = plt.subplots(1,2)

ax =axis[0]

YASPvals = df[3]
HSPvals = df[6]
GSPvals = df[9]

rects1 = ax.bar(ind,YASPvals,width,color='r')
rects2 = ax.bar(ind+width,HSPvals,width,color='g')
rects3 = ax.bar(ind+width*2,GSPvals,width,color='b')

ax.set_ylabel('Time to Solve')
ax.set_xticks(ind+width)
ax.set_xticklabels(names)
ax.legend((rects1[0],rects2[0],rects3[0]),('YASP','HSP','GSP'))

def autolabel(rects):
    for rect in rects:
        h = rect.get_height()
        ax.text(rect.get_x()+rect.get_width()/2.,1.05*h,'%d'%int(h),
                ha='center',va='bottom')
        
autolabel(rects1)
autolabel(rects2)
autolabel(rects3)


ax = axis[1]

YASPvals = df[4]
HSPvals = df[7]
GSPvals = df[10]

rects1 = ax.bar(ind,YASPvals,width,color='r')
rects2 = ax.bar(ind+width,HSPvals,width,color='g')
rects3 = ax.bar(ind+width*2,GSPvals,width,color='b')

ax.set_ylabel('Time to Solve')
ax.set_xticks(ind+width)
ax.set_xticklabels(names)
ax.legend((rects1[0],rects2[0],rects3[0]),('YASP','HSP','GSP'))
        
autolabel(rects1)
autolabel(rects2)
autolabel(rects3)

plt.savefig('Comparaison.png')
plt.show()